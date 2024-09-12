package com.dingdong.imageserver.auth.jwt;

import com.dingdong.imageserver.auth.student.StudentRepository;
import com.dingdong.imageserver.auth.teacher.TeacherRepository;
import com.dingdong.imageserver.exception.CustomException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dingdong.imageserver.response.ErrorStatus.INVALID_TOKEN;

@Component
@Slf4j
public class TokenProvider implements
        InitializingBean {

    public static final String AUTHORITIES = "auth";
    private final String secret;
    private SecretKey key;

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    public String stringSecret;
    private final long accessExpired;
    private final long refreshExpired;

    public TokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access_expired-time}") long accessExpired,
            @Value("${jwt.refresh_expired-time}") long refreshExpired,
            TeacherRepository teacherRepository, StudentRepository studentRepository) {
        this.secret = secret;
        this.accessExpired = accessExpired;
        this.refreshExpired = refreshExpired;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public void afterPropertiesSet() {
        byte[] decoded = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(decoded);
    }

    public String resolveToken(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }


    public boolean validateToken(String token) {
        if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
            token = token.substring(7);
        }

        JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
        try {
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            throw new CustomException(INVALID_TOKEN, "Invalid token " + e);
        } catch (ExpiredJwtException e) {
            throw new CustomException(INVALID_TOKEN, "Expired token " + e);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(INVALID_TOKEN, "Token not supported " + e);
        } catch (IllegalArgumentException e) {
            throw new CustomException(INVALID_TOKEN, "Invalid token " + e);
        }
    }

    public TokenResponse createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        Date accessExpiration = Date.from(issuedAt.plus(accessExpired, ChronoUnit.SECONDS));
        Date refreshExpiration = Date.from(issuedAt.plus(refreshExpired, ChronoUnit.SECONDS));

        var accessToken = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer("CD2024")
                .setIssuedAt(new Date())
                .setExpiration(accessExpiration)
                .claim("id", authentication.getName())
                .signWith(key, SignatureAlgorithm.HS512)
                .claim(AUTHORITIES, authorities)
                .compact();

        var refreshToken = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer("CD2024")
                .setIssuedAt(new Date())
                .setExpiration(refreshExpiration)
                .claim("id", authentication.getName())
                .signWith(key, SignatureAlgorithm.HS512)
                .claim(AUTHORITIES, authorities)
                .compact();


        return new TokenResponse(accessToken, refreshToken);
    }


    public Authentication resolveToken(String token) {

        if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
            token = token.substring(7);
        }

        JwtParser jwtParser = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build();
        Claims claims = jwtParser
                .parseClaimsJws(token)
                .getBody();

        String id = claims.get("id", String.class);

        Collection<SimpleGrantedAuthority> authorities = Stream.of(
                        String.valueOf(claims.get(AUTHORITIES)).split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());


        User principal = new User(id, "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
}