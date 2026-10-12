package br.com.bassi.monify.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public abstract class MonifyException extends RuntimeException {

    protected MonifyException(String mensagem) {
        super(mensagem);
    }

    public ProblemDetail toProblemDetail() {
        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        pd.setTitle("Erro interno do Monify");
        pd.setDetail("Entre em contato com o suporte");
        return pd;
    }
}
