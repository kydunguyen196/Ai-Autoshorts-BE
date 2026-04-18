package com.autoshorts.ai.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "app.queue.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public Declarables videoGenerationQueueDeclarables(AppProperties appProperties) {
        AppProperties.Queue queueProperties = appProperties.getQueue();

        TopicExchange mainExchange = new TopicExchange(queueProperties.getExchange(), true, false);
        TopicExchange deadLetterExchange = new TopicExchange(queueProperties.getDeadLetterExchange(), true, false);

        Queue mainQueue = org.springframework.amqp.core.QueueBuilder.durable(queueProperties.getQueue())
            .withArgument("x-dead-letter-exchange", queueProperties.getDeadLetterExchange())
            .withArgument("x-dead-letter-routing-key", queueProperties.getDeadLetterRoutingKey())
            .build();
        Queue deadLetterQueue = org.springframework.amqp.core.QueueBuilder.durable(queueProperties.getDeadLetterQueue()).build();

        Binding mainBinding = BindingBuilder
            .bind(mainQueue)
            .to(mainExchange)
            .with(queueProperties.getRoutingKey());
        Binding deadLetterBinding = BindingBuilder
            .bind(deadLetterQueue)
            .to(deadLetterExchange)
            .with(queueProperties.getDeadLetterRoutingKey());

        return new Declarables(mainExchange, deadLetterExchange, mainQueue, deadLetterQueue, mainBinding, deadLetterBinding);
    }

    @Bean(name = "videoJobRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory videoJobRabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        SimpleRabbitListenerContainerFactoryConfigurer configurer,
        RabbitTemplate rabbitTemplate,
        MessageConverter rabbitMessageConverter,
        AppProperties appProperties
    ) {
        AppProperties.Queue queueProperties = appProperties.getQueue();
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setConcurrentConsumers(queueProperties.getConsumerConcurrency());
        factory.setMaxConcurrentConsumers(queueProperties.getConsumerConcurrency());
        factory.setAdviceChain(retryInterceptor(rabbitTemplate, queueProperties));
        return factory;
    }

    private RetryOperationsInterceptor retryInterceptor(RabbitTemplate rabbitTemplate, AppProperties.Queue queueProperties) {
        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
            rabbitTemplate,
            queueProperties.getDeadLetterExchange(),
            queueProperties.getDeadLetterRoutingKey()
        );
        return RetryInterceptorBuilder.stateless()
            .maxAttempts(queueProperties.getMaxProcessingAttempts())
            .backOffOptions(
                queueProperties.getRetryInitialIntervalMs(),
                queueProperties.getRetryMultiplier(),
                queueProperties.getRetryMaxIntervalMs()
            )
            .recoverer(recoverer)
            .build();
    }
}
