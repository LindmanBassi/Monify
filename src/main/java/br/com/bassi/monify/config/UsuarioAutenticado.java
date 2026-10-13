package br.com.bassi.monify.config;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// o ID do usuário vem sempre do token, nunca do request
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal(expression = "T(java.util.UUID).fromString(subject)")
public @interface UsuarioAutenticado {
}
