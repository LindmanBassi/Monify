package br.com.bassi.monify.handler;

import br.com.bassi.monify.dto.CampoInvalidoResponse;
import br.com.bassi.monify.exception.MonifyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Parâmetros inválidos");
        pd.setDetail("Valor inválido para o parâmetro '" + e.getName() + "'");
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Requisição inválida");
        pd.setDetail("Corpo da requisição ausente ou malformado");
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
