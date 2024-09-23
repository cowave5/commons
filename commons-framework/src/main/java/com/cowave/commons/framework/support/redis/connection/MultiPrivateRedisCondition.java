package com.cowave.commons.framework.support.redis.connection;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 *
 * @author shanhuiming
 *
 */
public class MultiPrivateRedisCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
		RedisProperties privateRedis = Binder.get(context.getEnvironment()).bind(
				"spring.redis.private", RedisProperties.class).orElse(null);
		return privateRedis != null;
	}
}
