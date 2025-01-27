/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.autoconfigure.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for Config bootstrap configuration.
 *
 * @author Jisha Abubaker
 * @author João André Martins
 * @author Chengyuan Zhao
 */
class GcpConfigBootstrapConfigurationTest {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpConfigBootstrapConfiguration.class))
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void testConfigurationValueDefaultsAreAsExpected() {
		this.contextRunner.withPropertyValues("spring.cloud.gcp.config.enabled=true")
				.run(context -> {
					GcpConfigProperties config = context.getBean(GcpConfigProperties.class);
					assertThat(config.getName()).isEqualTo("application");
					assertThat(config.getProfile()).isEqualTo("default");
					assertThat(config.getTimeoutMillis()).isEqualTo(60000);
					assertThat(config.isEnabled()).isTrue();
		});
	}

	@Test
	void testConfigurationValuesAreCorrectlyLoaded() {
		this.contextRunner.withPropertyValues("spring.application.name=myapp",
				"spring.profiles.active=prod",
				"spring.cloud.gcp.config.timeoutMillis=120000",
				"spring.cloud.gcp.config.enabled=true",
				"spring.cloud.gcp.config.project-id=pariah")
				.run(context -> {
					GcpConfigProperties config = context.getBean(GcpConfigProperties.class);
					assertThat(config.getName()).isEqualTo("myapp");
					assertThat(config.getProfile()).isEqualTo("prod");
					assertThat(config.getTimeoutMillis()).isEqualTo(120000);
					assertThat(config.isEnabled()).isTrue();
					assertThat(config.getProjectId()).isEqualTo("pariah");
				});
	}

	@Test
	void testConfigurationDisabled() {
		this.contextRunner.run(context ->
				assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
						context.getBean(GcpConfigProperties.class)));
	}

	private static class TestConfiguration {

		@Bean
		public GoogleConfigPropertySourceLocator googleConfigPropertySourceLocator() {
			return mock(GoogleConfigPropertySourceLocator.class);
		}
	}
}
