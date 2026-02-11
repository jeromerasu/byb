package com.workoutplanner.config;

import com.workoutplanner.security.JwtAuthenticationFilter;
import com.workoutplanner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final UserService userService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;

    // Security is ENABLED by default. Only disabled when explicitly setting BETA=true
    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public SecurityConfig(UserService userService, JwtAuthenticationFilter jwtAuthenticationFilter, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.passwordEncoder = passwordEncoder;
    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // SECURITY-FIRST: Authentication is ENABLED by default
        if (betaMode) {
            logger.warn("⚠️  BETA MODE ACTIVE: All authentication DISABLED! Only use for development. Set BETA=false or remove BETA for production.");
            http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
        } else {
            logger.info("🔒 SECURE MODE: Authentication enabled for protected endpoints (default & recommended).");

            // Normal mode: apply standard security rules
            http.authorizeHttpRequests(authz -> authz
                // Public endpoints (includes mobile auth endpoints)
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/workout-plans/health").permitAll()
                .requestMatchers("/api/v1/diet-plans/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()

                // Protected endpoints
                .requestMatchers("/api/v1/workout-plans/generate").authenticated()
                .requestMatchers("/api/v1/workout-plans/save").authenticated()
                .requestMatchers("/api/v1/workout-plans/generate-and-save").authenticated()
                .requestMatchers("/api/v1/diet-plans/generate").authenticated()
                .requestMatchers("/api/v1/diet-plans/save").authenticated()
                .requestMatchers("/api/v1/diet-plans/generate-and-save").authenticated()

                // Admin only endpoints
                .requestMatchers("/api/v1/users/admin/**").hasRole("ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        // Security headers configuration
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.deny()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}