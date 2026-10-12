package com.servicioagua.security;

import com.servicioagua.common.exception.RateLimitException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 5;
    private static final long VENTANA_MS = 15 * 60 * 1000L;

    private final Map<String, Deque<Long>> intentos = new ConcurrentHashMap<>();

    public void verificarBloqueo(String username) {
        Deque<Long> cola = intentos.get(username);
        if (cola == null) {
            return;
        }
        synchronized (cola) {
            purgar(cola);
            if (cola.size() >= MAX_INTENTOS) {
                throw new RateLimitException(
                        "Demasiados intentos fallidos. Intente de nuevo en unos minutos");
            }
        }
    }

    public void registrarFallo(String username) {
        Deque<Long> cola = intentos.computeIfAbsent(username, k -> new ArrayDeque<>());
        synchronized (cola) {
            purgar(cola);
            cola.addLast(System.currentTimeMillis());
        }
    }

    public void limpiar(String username) {
        intentos.remove(username);
    }

    private void purgar(Deque<Long> cola) {
        long limite = System.currentTimeMillis() - VENTANA_MS;
        while (!cola.isEmpty() && cola.peekFirst() < limite) {
            cola.pollFirst();
        }
    }
}
