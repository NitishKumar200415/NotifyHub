package com.notifyhub.ratelimit;

import com.notifyhub.auth.AppUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    private final StringRedisTemplate redisTemplate;

    public RateLimitingFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(
            @NonNull HttpServletRequest request
    ) {
        return !request.getRequestURI()
                .startsWith("/api/notifications");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal() instanceof AppUserDetails userDetails) {

            Long userId = userDetails.getAppUser().getId();

            String currentMinute =
                    String.valueOf(Instant.now().getEpochSecond() / 60);

            String key = "ratelimit:" + userId + ":" + currentMinute;

            Long requestCount =
                    redisTemplate.opsForValue().increment(key);

            if (requestCount != null && requestCount == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }

            if (requestCount != null
                    && requestCount > MAX_REQUESTS_PER_MINUTE) {

                response.setStatus(429);


                response.setContentType("application/json");

                response.getWriter().write("""
                        {
                          "status": 429,
                          "message": "Rate limit exceeded. Try again later."
                        }
                        """);

                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}