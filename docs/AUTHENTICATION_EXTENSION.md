# Spätere Authentifizierung ergänzen

Die aktuelle Schulungsversion besitzt bewusst keine Anmeldung. Die Struktur wurde so gewählt, dass Spring Security später ergänzt werden kann, ohne Controller oder Fachlogik umzubauen.

## Empfohlene Ausbaustufen

1. **API-Key-Demo** für einen kurzen Unterrichtsblock.
2. **JWT/OAuth2 Resource Server** für eine realistischere Enterprise-Demo.
3. Rollen wie `TASK_READER` und `TASK_EDITOR` an den HTTP-Endpunkten prüfen.

## Benötigte Dependency für JWT

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```

## Beispiel einer späteren Security-Konfiguration

```java
@Configuration
@EnableMethodSecurity
class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tasks/**").hasAuthority("SCOPE_tasks.read")
                        .requestMatchers("/api/tasks/**").hasAuthority("SCOPE_tasks.write")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}
```

## Wichtige Sicherheitsregeln

- Schlüssel und Tokens nie ins Repository committen.
- Secrets über Kubernetes Secrets oder einen Secret Store bereitstellen.
- In Produktion HTTPS erzwingen; diese lokale Demo lässt HTTPS bewusst aus.
- Zugriffsrechte nach dem Least-Privilege-Prinzip vergeben.
- Authentifizierung und Autorisierung separat testen.
