package br.com.bassi.monify.handler;

import br.com.bassi.monify.dto.CampoInvalidoResponse;
import br.com.bassi.monify.exception.MonifyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MonifyException.class)
    public ProblemDetail handleMonifyException(MonifyException e) {

        return e.toProblemDetail();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

        var camposInvalidos = e.getFieldErrors()
                .stream()
                .map(fe -> new CampoInvalidoResponse(fe.getField(), fe.getDefaultMessage()))
                .toList();

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Parâmetros inválidos");
        pd.setDetail("Existem campos inválidos na requisição");
        pd.setProperty("campos-invalidos", camposInvalidos);
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException e) {

        var pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Não autenticado");
        pd.setDetail("Token de acesso ausente, inválido ou expirado");
        return pd;
    }
}
