package jdriven.training.mongodb.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.UUID;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

@Configuration
@EnableConfigurationProperties(MongoDbProperties.class)
public class MongoDbConfig {

    private final MongoDbProperties properties;

    public MongoDbConfig(MongoDbProperties properties) {
        this.properties = properties;
    }

	@Bean
	MongoDbIdProvider uuidProvider() {
		return UUID::randomUUID;
	}

	@Bean
	MongoTemplate mongoTemplate(MongoClient mongoDbFactory) {
		return new MongoTemplate(mongoDbFactory, properties.database());
	}

    @Bean
	MongoClient mongoClient() {
        ConnectionString connection = new ConnectionString(properties.url());

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        MongoClientSettings settings = MongoClientSettings.builder()
			.uuidRepresentation(UuidRepresentation.STANDARD)
            .applyConnectionString(connection)
            .codecRegistry(codecRegistry)
            .build();

        try {
            return MongoClients.create(settings);
        } catch (Exception e) {
            throw new IllegalStateException("Could not start application due to MongoDb startup failure", e);
        }
    }
}
