package com.managify.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.managify.projects.dto.response.ProjectResponse;
import com.managify.projects.dto.response.ProjectTrackerResponse;
import com.managify.projects.dto.response.WorkerItemsResponse;
import com.managify.projects.dto.response.WorkerResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed Spring Cache.
 *
 * <ul>
 *   <li>Caches hold response DTOs only (never JPA entities), each with its own typed JSON serializer.</li>
 *   <li>Writes evict the affected entries after commit (see {@code ProjectCacheEvictor}); TTLs only bound staleness.</li>
 *   <li>Redis failures are logged and the call falls through to MySQL, so the API keeps working without Redis.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final String KEY_PREFIX = "managify::";

    @Value("${app.cache.ttl.project-tracker:30m}")
    private Duration projectTrackerTtl;

    @Value("${app.cache.ttl.project:1h}")
    private Duration projectTtl;

    @Value("${app.cache.ttl.worker:1h}")
    private Duration workerTtl;

    @Value("${app.cache.ttl.worker-items:10m}")
    private Duration workerItemsTtl;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = cacheObjectMapper();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(Map.of(
                        CacheNames.PROJECT_TRACKER, typed(mapper, ProjectTrackerResponse.class, projectTrackerTtl),
                        CacheNames.PROJECT, typed(mapper, ProjectResponse.class, projectTtl),
                        CacheNames.WORKER, typed(mapper, WorkerResponse.class, workerTtl),
                        CacheNames.WORKER_ITEMS, typed(mapper, WorkerItemsResponse.class, workerItemsTtl)))
                .disableCreateOnMissingCache()
                .build();
    }

    /** Log and carry on: a Redis outage turns every cache call into a miss instead of an error. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    private static ObjectMapper cacheObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // An entry written before a DTO lost a field still reads back after a deploy.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static RedisCacheConfiguration base(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .prefixCacheNameWith(KEY_PREFIX)
                .disableCachingNullValues();
    }

    private static <T> RedisCacheConfiguration typed(ObjectMapper mapper, Class<T> type, Duration ttl) {
        return base(ttl).serializeValuesWith(
                SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, type)));
    }
}
