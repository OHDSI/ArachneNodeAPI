/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: December 16, 2016
 *
 */

package com.odysseusinc.arachne.datanode.security;

import com.odysseusinc.arachne.datanode.model.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenUtils {

    private final Logger logger = Logger.getLogger(getClass());

    @Value("${datanode.jwt.expiration}")
    private Long expiration;

    @Value("${datanode.jwt.secret}")
    private String secret;

    private ConcurrentHashMap<String, Date> invalidatedTokens = new ConcurrentHashMap<>();

    public boolean isExpired(String token) {

        boolean expired;
        try {
            Claims claims = getClaimsFromToken(token);
            expired = claims.getExpiration().getTime() * 1000 < new Date().getTime();
        } catch (Exception ex) {
            expired = true;
        }
        return expired;
    }

    public String getUsernameFromToken(String token) {

        String username;
        try {
            final Claims claims = getClaimsFromToken(token);
            username = claims.getSubject();
        } catch (Exception ex) {
            username = null;
        }
        return username;
    }

    public String getUUIDFromToken(String token) {

        String uuid = null;
        try {
            final Claims claims = getClaimsFromToken(token);
            uuid = claims.get("uuid", String.class);
        } catch (Exception ex) {
            uuid = null;
        }
        return uuid;
    }

    public Date getCreatedDateFromToken(String token) {

        return getCreatedDateFromToken(token, true);
    }

    public Date getCreatedDateFromToken(String token, Boolean signed) {

        Date created;
        try {
            final Claims claims = getClaimsFromToken(token, signed);
            created = new Date((Long) claims.get("created"));
        } catch (Exception ex) {
            created = null;
        }
        return created;
    }

    public Date getExpirationDateFromToken(String token) {

        Date expiration;
        try {
            final Claims claims = getClaimsFromToken(token);
            expiration = claims.getExpiration();
        } catch (Exception ex) {
            expiration = null;
        }
        return expiration;
    }


    private Claims getClaimsFromToken(String token) {

        return getClaimsFromToken(token, true);
    }

    private Claims getClaimsFromToken(String token, Boolean signed) {

        Claims claims;
        try {
            if (signed) {
                claims = Jwts.parser()
                        .setSigningKey(secret)
                        .parseClaimsJws(token)
                        .getBody();
            } else {
                claims = Jwts.parser()
                        .parseClaimsJwt(token)
                        .getBody();
            }
        } catch (Exception ex) {
            claims = null;
        }
        return claims;
    }

    private Date generateCurrentDate() {

        return new Date(System.currentTimeMillis());
    }

    private Date generateExpirationDate(Date created) {

        return new Date(created.getTime() + expiration * 1000);
    }

    private Boolean isTokenExpired(String token) {

        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(generateCurrentDate());
    }

    private Boolean isCreatedBeforeLastPasswordReset(Date created, Date lastPasswordReset) {

        return (lastPasswordReset != null && created.before(lastPasswordReset));
    }


    public String generateToken(User user, Date created) {

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("sub", user.getEmail());
        claims.put("created", created);
        claims.put("uuid", UUID.randomUUID().toString());
        return generateToken(claims);
    }

    private String generateToken(Map<String, Object> claims) {

        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(generateExpirationDate((Date) claims.get("created")))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public Boolean canTokenBeRefreshed(String token, Date lastPasswordReset) {

        final Date created = getCreatedDateFromToken(token);
        String uuid = getUUIDFromToken(token);
        return (!(invalidatedTokens.containsKey(uuid))
                && !(isCreatedBeforeLastPasswordReset(created, lastPasswordReset))
                && (!(isTokenExpired(token))));
    }

    public String refreshToken(String token) {

        String refreshedToken = null;

        try {
            final Claims claims = getClaimsFromToken(token);
            claims.put("created", generateCurrentDate());
            refreshedToken = generateToken(claims);
        } catch (Exception e) {
            refreshedToken = null;
        }
        return refreshedToken;
    }

    public Boolean validateToken(String token, UserDetails user) {

        String uuid = getUUIDFromToken(token);
        boolean result = false;
        if (!invalidatedTokens.containsKey(uuid)) {
            final String username = getUsernameFromToken(token);
            result = (username.equals(user.getUsername()) && !(isTokenExpired(token)));
        }
        return result;
    }

    public void addInvalidateToken(String token) {

        String uuid = getUUIDFromToken(token);
        Date expirationDate = getExpirationDateFromToken(token);
        Date now = new Date();
        if (expirationDate.after(now)) {
            invalidatedTokens.put(uuid, expirationDate);
        }
        //remove old tokens
        for (Iterator<Map.Entry<String, Date>> iterator = invalidatedTokens.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Date> stringDateEntry = iterator.next();
            if (now.after(stringDateEntry.getValue())) {
                iterator.remove();
            }
        }
    }

}
