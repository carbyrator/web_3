package ru.studentlab.students.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import java.util.List;
import ru.studentlab.students.service.JwtService;
import ru.studentlab.students.repository.UserRepository;

// Здесь задаём открытые адреса и права для каждой роли
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain security(HttpSecurity http, JwtService jwt,
                                        UserRepository users, SecurityErrors errors) throws Exception {
        JwtFilter jwtFilter = new JwtFilter(jwt, users, errors);
        // Стрелка передаёт Spring небольшой блок настроек
        http.csrf(csrf -> csrf.disable()); // Вход проверяем по токену из заголовка
        // Сервер не хранит сеанс входа, каждый запрос содержит токен
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(handler -> handler
                .authenticationEntryPoint(errors).accessDeniedHandler(errors));
        // Чтение доступно обеим ролям, изменение только администратору
        http.authorizeHttpRequests(rules -> rules
                .requestMatchers("/", "/*.html", "/js/**", "/css/**", "/assets/**", "/error",
                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/requests", "/api/requests/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.valueOf("QUERY"), "/api/requests").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/requests", "/api/requests/**").hasRole("ADMIN")
                .anyRequest().authenticated());
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer firewall() {
        // Разрешаем метод QUERY из предыдущей лабораторной
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowedHttpMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "QUERY"));
        return web -> web.httpFirewall(firewall);
    }
}
