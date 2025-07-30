package com.open.unicorn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.ArrayList;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@SpringBootApplication
@EnableScheduling
public class UnicornApplication {

	@RestController
	@RequestMapping("/api/auth")
	public class AuthController {

		@Autowired
		private UserRepository userRepository;

		@Autowired
		private PasswordEncoder passwordEncoder;

		// Use fully qualified name for User entity in AuthController
		@PostMapping("/register")
		public String registerUser(@RequestBody com.open.unicorn.User user) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			userRepository.save(user);
			return "User registered successfully";
		}

		@PostMapping("/login")
		public ResponseEntity<String> loginUser(@RequestBody com.open.unicorn.User loginRequest) {
			try {
				System.out.println("Login attempt for email: " + loginRequest.getEmail());
				com.open.unicorn.User user = userRepository.findByEmail(loginRequest.getEmail());
				
				if (user == null) {
					System.out.println("User not found for email: " + loginRequest.getEmail());
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
				}
				
				System.out.println("User found: " + user.getEmail());
				boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
				System.out.println("Password matches: " + passwordMatches);
				
				if (passwordMatches) {
					String token = Jwts.builder()
							.setSubject(user.getEmail())
							.setExpiration(new Date(System.currentTimeMillis() + 864_000_000)) // 10 days
							.signWith(getSigningKey(), SignatureAlgorithm.HS512)
							.compact();
					System.out.println("Login successful for: " + user.getEmail());
					return ResponseEntity.ok(token);
				} else {
					System.out.println("Password does not match for: " + user.getEmail());
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
				}
			} catch (Exception e) {
				System.err.println("Login error: " + e.getMessage());
				e.printStackTrace();
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: " + e.getMessage());
			}
		}
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// Use fully qualified name for UserDetails in userDetailsService
	@Bean
	public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
		UserDetails user = org.springframework.security.core.userdetails.User.builder()
			.username("user")
			.password(passwordEncoder.encode("password"))
			.roles("USER")
			.build();
		return new InMemoryUserDetailsManager(user);
	}

	@Configuration
	public class SecurityConfig {

		@Bean
		public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
			http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
					.requestMatchers("/api/auth/**").permitAll()
					.requestMatchers("/", "/index.html", "/login.html", "/register.html", "/dashboard.html", "/uws-s3.html", "/uws-compute.html", "/uws-lambda.html", "/uws-rdb.html", "/uws-sqs.html", "/uws-nosql.html", "/css/**", "/js/**", "/images/**").permitAll()
					.requestMatchers("/h2-console/**").permitAll()
					.requestMatchers("/api/uws-s3/**").authenticated()
					.requestMatchers("/api/uws-compute/**").authenticated()
					.requestMatchers("/api/lambdas/**").authenticated()
					.requestMatchers("/api/uws-rdb/**").authenticated()
					.requestMatchers("/api/uws-sqs/**").authenticated()
					.requestMatchers("/api/nosql/**").authenticated()
					.anyRequest().authenticated()
				)
				.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())) // For H2 console
				.addFilterBefore(new JWTAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);
			
			return http.build();
		}

		@Bean
		public AuthenticationManager authManager(HttpSecurity http) 
			throws Exception {
			AuthenticationManagerBuilder authenticationManagerBuilder = 
				http.getSharedObject(AuthenticationManagerBuilder.class);
			authenticationManagerBuilder.userDetailsService(userDetailsService(passwordEncoder()));
			return authenticationManagerBuilder.build();
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(UnicornApplication.class, args);
	}

	public static class JWTAuthorizationFilter extends OncePerRequestFilter {
		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
				throws IOException, ServletException {
			
			System.out.println("JWT Filter: Processing request to " + request.getRequestURI());
			String header = request.getHeader("Authorization");
			System.out.println("JWT Filter: Authorization header: " + (header != null ? "present" : "null"));

			if (header == null || !header.startsWith("Bearer ")) {
				System.out.println("JWT Filter: No Bearer token found, continuing without authentication");
				filterChain.doFilter(request, response);
				return;
			}

			UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
			
			if (authentication != null) {
				System.out.println("JWT Filter: Authentication successful for user: " + authentication.getName());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} else {
				System.out.println("JWT Filter: Authentication failed");
			}
			
			filterChain.doFilter(request, response);
		}

		private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
			String token = request.getHeader("Authorization");
			if (token != null) {
				try {
					System.out.println("JWT Filter: Parsing token...");
					// parse the token.
					String user = Jwts.parserBuilder()
							.setSigningKey(getSigningKey())
							.build()
							.parseClaimsJws(token.replace("Bearer ", ""))
							.getBody()
							.getSubject();

					if (user != null) {
						System.out.println("JWT Filter: Token parsed successfully for user: " + user);
						return new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
					}
				} catch (Exception e) {
					System.err.println("JWT Filter: Error parsing JWT token: " + e.getMessage());
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	private static Key getSigningKey() {
		// Use a consistent but secure key for HS512 (at least 512 bits = 64 bytes)
		// This is a base64-encoded key that's long enough for HS512
		String secretKey = "dG9rZW5fc2VjcmV0X2tleV9mb3JfdW5pY29ybl9hcHBsaWNhdGlvbl90aGF0X2lzX3NlY3VyZV9hbmRfbG9uZ19lbm91Z2hfZm9yX2hzNTEyX2FsZ29yaXRobQ==";
		byte[] keyBytes = java.util.Base64.getDecoder().decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
