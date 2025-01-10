package net.jpmchase.chroniclesdk.config;

import com.jpmorgan.moneta.boot.security.adfs.client.AdfsClientTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Profile("local")
@Configuration
public class OAuthAdfsclientConfig {

    @Value("${app.adfs.client-id-for-certificate-auth}")
    private String adfsClientId;

    @Value("${app.adfs.resource-uri}")
    private String adfsProtectedResourceUri;

    @Value("${app.adfs.provider-url}")
    private String adfsProviderUrl;

    @Value("${app.adfs.client-key-store-classpath}")
    private String adfsClientKeystoreclasspath;

    @Value("${app.adfs.client-key-store-secret}")
    private String adfsClientKeystoreSecret;

    @Value("${app.adfs.client-token-provider-id}")
    private String adfsClientTokenProviderId;

    @Bean
    public AdfsClientTokenProvider adfsClientTokenProvider() throws Exception {
        AdfsClientTokenProvider adfsClientTokenProvider = AdfsClientTokenProvider.builder()
                .id(adfsClientTokenProviderId)
                .clientId(adfsClientId)
                .resourceUri(adfsProtectedResourceUri)
                .providerUrl(adfsProviderUrl)
                .enableX509CertificateAuthentication(
                        new ClassPathResource(adfsClientKeystoreclasspath).getInputStream(),
                        adfsClientKeystoreSecret,
                        null)
                .build();
        log.info("Get ADFS Token {}", adfsClientTokenProvider.getAdfsTokenResponse().getAccessToken());
        return adfsClientTokenProvider;
    }
}


