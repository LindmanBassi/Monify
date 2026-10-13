package br.com.bassi.monify.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class ConflitoException extends MonifyException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public ProblemDetail toProblemDetail() {
        var pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);

        pd.setTitle("Conflito");
        pd.setDetail(getMessage());
        return pd;
    }
}
