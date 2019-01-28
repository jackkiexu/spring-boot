SpringBoot 常见注解
  6. @Conditional: 条件匹配注解, 主要当 matched 了才会加载对应的类到 Spring IOC 中
  1. @ConditionalOnClass: 在 classpath 上存在指定的 class, 才会将 bean 注入到 Spring 上下文中
  2. @ConditionalOnBean:  only matches when the specified bean classes and/or names are already contained in the BeanFactory
  3. @ConditionalOnProperty: 在配置中心是否存在对应的配置
  4. @EnableConfigurationProperties
  5. @Import

  7. @ConditionalOnSingleCandidate
  8. @AutoConfigureOrder
  9. @AutoConfigureAfter
  10. @AutoConfigureBefore
  11. @EnableAutoConfiguration: 其本质上是一个 @Import, 所以其还是被 ConfigurationClassParser 来进行处理
  12. @SpringBootApplication: 本质上也是一个 @ComponentScan, @EnableAutoConfiguration, @Import, @AutoConfigurationPackage, @Configuration, @SpringBootConfiguration, @Configuration

SpringBoot 重要的类
  1. ApplicationContextInitializer
  2. SpringApplicationRunListener