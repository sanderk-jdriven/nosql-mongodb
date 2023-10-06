package jdriven.training.mongodb.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mongodb")
public record MongoDbProperties(
    @NotNull
	String url,
	@NotNull
	String database
) {
}
