package br.com.bassi.monify.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class RegraNegocioException extends MonifyException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }

    @Override
    public ProblemDetail toProblemDetail() {
        var pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);

        pd.setTitle("Regra de negócio violada");
        pd.setDetail(getMessage());
        return pd;
    }
}
