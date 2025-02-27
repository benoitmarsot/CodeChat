package com.unbumpkin.codechat.config;

import com.unbumpkin.codechat.security.CustomAuthentication;
import com.unbumpkin.codechat.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtUtil jwtUtil;
    private final IpWhitelistFilter ipWhitelistFilter;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.ipWhitelistFilter = new IpWhitelistFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
        //Uses the CORS settings defined in WebConfig
        .cors(Customizer.withDefaults())
        //Disables CSRF protection (common for stateless REST APIs using tokens)
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // Allows OPTIONS requests (needed for CORS preflight)
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // Public authentication endpoints
            .requestMatchers("/api/v1/auth/**").permitAll()
            // Public Swagger documentation endpoints
            .requestMatchers(
                "/swagger-ui.html", 
                "/swagger-ui/**", 
                "/v3/api-docs/**", 
                "/api-docs/**", 
                "/api-docs.yaml"
            ).permitAll()
            // All other requests need authentication
            .anyRequest().authenticated()
        )
        //Configures stateless sessions (no session cookies): using JWT-based authentication
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        //Adds IP whitelist filter first
        .addFilterBefore(ipWhitelistFilter, UsernamePasswordAuthenticationFilter.class) // Add IP whitelist filter before JWT filter
        //Adds JWT filter after IP whitelist filter
        .addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}

@Component
class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String username = jwtUtil.validateToken(token);
            int userId = jwtUtil.getUserIdFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            UserDetails userDetails = User.withUsername(username).password("").roles(role).build();
            CustomAuthentication auth = new CustomAuthentication(userDetails, null, userDetails.getAuthorities(), userId);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}


// @Configuration
// @EnableWebSecurity
// public class SecurityConfig {

//     @Bean
//     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//         http
//             .authorizeRequests(authorizeRequests ->
//                 authorizeRequests
//                     .requestMatchers("/api/openai/files/**").permitAll() // Allow access to your endpoints
//                     .anyRequest().authenticated()
//             )
//             .csrf().disable(); // Disable CSRF for simplicity, enable it in production with proper configuration

//         return http.build();
//     }
// }