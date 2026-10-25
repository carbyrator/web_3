package ru.studentlab.students.security;

import ru.studentlab.students.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserReader;
import ru.studentlab.students.service.port.AccessTokenReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Проверяем токен до обращения к защищённому адресу
public class JwtFilter extends OncePerRequestFilter {
    private final AccessTokenReader jwt;
    private final UserReader users;
    private final SecurityErrors errors;

    public JwtFilter(AccessTokenReader jwt, UserReader users, SecurityErrors errors) {
        this.jwt = jwt;
        this.users = users;
        this.errors = errors;
    }

    // Читаем заголовок Authorization и проверяем учётную запись
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ")) {
                    throw new IllegalArgumentException();
                }
                String token = header.substring(7);
                String username = jwt.readAccessUsername(token);
                User user = users.findByUsername(username);
                if (user == null) {
                    throw new IllegalArgumentException();
                }
                // Spring ожидает имя роли с префиксом ROLE_
                List<GrantedAuthority> roles = new ArrayList<>();
                roles.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, roles);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException | IllegalArgumentException e) {
                errors.write(response, 401, "Access-токен недействителен или истёк");
                return;
            }
        }
        // Продолжаем запрос, дальнейшие права проверяет Spring Security
        chain.doFilter(request, response);
    }
}
