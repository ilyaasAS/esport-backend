package org.example.oracle;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.autoconfigure.vectorstore.chroma.ChromaApiProperties;
import org.springframework.ai.autoconfigure.vectorstore.chroma.ChromaConnectionDetails;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Configuration adaptative pour le client ChromaDB.
 * - Assure la normalisation de l'URI d'hôte (injection du schéma http:// si omis).
 * - Utilise SimpleClientHttpRequestFactory (HTTP/1.1 strict) pour éliminer le header
 *   'Upgrade: h2c' de Java HTTP Client qui provoque l'erreur 'Unsupported upgrade request' sur Uvicorn/Chroma.
 */
@Configuration
@ConditionalOnClass({ChromaConnectionDetails.class, ChromaApi.class})
public class ChromaConfiguration {

    @Bean
    public ChromaConnectionDetails chromaConnectionDetails(ChromaApiProperties properties) {
        return new ChromaConnectionDetails() {
            @Override
            public String getHost() {
                String host = properties.getHost();
                if (host != null && !host.isBlank() && !host.startsWith("http://") && !host.startsWith("https://")) {
                    return "http://" + host;
                }
                return host;
            }

            @Override
            public int getPort() {
                return properties.getPort();
            }

            @Override
            public String getKeyToken() {
                return properties.getKeyToken();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ChromaApi chromaApi(
            ChromaApiProperties apiProperties,
            ChromaConnectionDetails connectionDetails,
            ObjectMapper objectMapper
    ) {
        String chromaUrl = String.format("%s:%s", connectionDetails.getHost(), connectionDetails.getPort());
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory());

        ChromaApi chromaApi = new ChromaApi(chromaUrl, builder, objectMapper);
        if (StringUtils.hasText(connectionDetails.getKeyToken())) {
            chromaApi.withKeyToken(connectionDetails.getKeyToken());
        }
        if (StringUtils.hasText(apiProperties.getUsername()) && StringUtils.hasText(apiProperties.getPassword())) {
            chromaApi.withBasicAuthCredentials(apiProperties.getUsername(), apiProperties.getPassword());
        }
        return chromaApi;
    }
}
