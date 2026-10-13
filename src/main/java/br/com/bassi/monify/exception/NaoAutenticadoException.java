package br.com.bassi.monify.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class NaoAutenticadoException extends MonifyException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public ProblemDetail toProblemDetail() {
        var pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

        pd.setTitle("Não autenticado");
        pd.setDetail(getMessage());
        return pd;
    }
}
