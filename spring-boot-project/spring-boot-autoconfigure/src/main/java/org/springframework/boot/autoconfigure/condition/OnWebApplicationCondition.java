/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.condition;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link Condition} that checks for the presence or absence of
 * {@link WebApplicationContext}.
 *
 * @author Dave Syer
 * @see ConditionalOnWebApplication
 * @see ConditionalOnNotWebApplication
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnWebApplicationCondition extends SpringBootCondition {

	private static final String WEB_CONTEXT_CLASS = "org.springframework.web.context."
			+ "support.GenericWebApplicationContext";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		// 检查是否被 @ConditionalOnWebApplication 注解修饰
		boolean required = metadata.isAnnotated(ConditionalOnWebApplication.class.getName());
		// 判断是否是WebApplication
		ConditionOutcome outcome = isWebApplication(context, metadata, required);
		if (required && !outcome.isMatch()) {
			// 如果有@ConditionalOnWebApplication 注解,但是不是WebApplication环境,则返回不匹配
			return ConditionOutcome.noMatch(outcome.getConditionMessage());
		}
		if (!required && outcome.isMatch()) {
			// 如果没有被@ConditionalOnWebApplication 注解,但是是WebApplication环境,则返回不匹配
			return ConditionOutcome.noMatch(outcome.getConditionMessage());
		}
		// 如果被@ConditionalOnWebApplication 注解,并且是WebApplication环境,则返回不匹配
		return ConditionOutcome.match(outcome.getConditionMessage());
	}

	private ConditionOutcome isWebApplication(ConditionContext context, AnnotatedTypeMetadata metadata, boolean required) {
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnWebApplication.class, required ? "(required)" : "");
		Type type = deduceType(metadata);
		if (Type.SERVLET == type) {
			return isServletWebApplication(context);
		}
		else if (Type.REACTIVE == type) {
			return isReactiveWebApplication(context);
		}
		else {
			ConditionOutcome servletOutcome = isServletWebApplication(context);
			if (servletOutcome.isMatch() && required) {
				return new ConditionOutcome(servletOutcome.isMatch(), message.because(servletOutcome.getMessage()));
			}
			ConditionOutcome reactiveOutcome = isReactiveWebApplication(context);
			if (reactiveOutcome.isMatch() && required) {
				return new ConditionOutcome(reactiveOutcome.isMatch(), message.because(reactiveOutcome.getMessage()));
			}
			boolean finalOutcome = (required
					? servletOutcome.isMatch() && reactiveOutcome.isMatch()
					: servletOutcome.isMatch() || reactiveOutcome.isMatch());
			return new ConditionOutcome(finalOutcome,
					message.because(servletOutcome.getMessage()).append("and").append(reactiveOutcome.getMessage()));
		}
	}

	private ConditionOutcome isServletWebApplication(ConditionContext context) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("");
		// 判断GenericWebApplicationContext是否在类路径中,如果不存在,则返回不匹配
		if (!ClassUtils.isPresent(WEB_CONTEXT_CLASS, context.getClassLoader())) {
			return ConditionOutcome
					.noMatch(message.didNotFind("web application classes").atAll());
		}
		// 容器里是否有名为session的scope,如果存在,则返回匹配
		if (context.getBeanFactory() != null) {
			String[] scopes = context.getBeanFactory().getRegisteredScopeNames();
			if (ObjectUtils.containsElement(scopes, "session")) {
				return ConditionOutcome.match(message.foundExactly("'session' scope"));
			}
		}
		// Environment 是否为 ConfigurableWebEnvironment,如果是的话,则返回匹配
		if (context.getEnvironment() instanceof ConfigurableWebEnvironment) {
			return ConditionOutcome
					.match(message.foundExactly("ConfigurableWebEnvironment"));
		}
		// 当前ResourceLoader是否为WebApplicationContext,如果是,则返回匹配
		if (context.getResourceLoader() instanceof WebApplicationContext) {
			return ConditionOutcome.match(message.foundExactly("WebApplicationContext"));
		}
		return ConditionOutcome.noMatch(message.because("not a servlet web application"));
	}

	private ConditionOutcome isReactiveWebApplication(ConditionContext context) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("");
		if (context.getEnvironment() instanceof ConfigurableReactiveWebEnvironment) {
			return ConditionOutcome
					.match(message.foundExactly("ConfigurableReactiveWebEnvironment"));
		}
		if (context.getResourceLoader() instanceof ReactiveWebApplicationContext) {
			return ConditionOutcome
					.match(message.foundExactly("ReactiveWebApplicationContext"));
		}
		return ConditionOutcome
				.noMatch(message.because("not a reactive web application"));
	}

	private Type deduceType(AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ConditionalOnWebApplication.class.getName());
		if (attributes != null) {
			return (Type) attributes.get("type");
		}
		return Type.ANY;
	}

}
