package ru.studentlab.students.security;

import io.jsonwebtoken.JwtException;
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
import ru.studentlab.students.repository.UserRepository;
import ru.studentlab.students.service.JwtService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final SecurityErrors errors;

    public JwtFilter(JwtService jwt, UserRepository users, SecurityErrors errors) {
        this.jwt = jwt;
        this.users = users;
        this.errors = errors;
    }

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
                if (!jwt.isAccessToken(token)) {
                    throw new IllegalArgumentException();
                }
                String username = jwt.getUsername(token);
                User user = users.findByUsername(username);
                if (user == null) {
                    throw new IllegalArgumentException();
                }
                List<GrantedAuthority> roles = new ArrayList<>();
                roles.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, roles);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                errors.write(response, 401, "Access-токен недействителен или истёк");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
