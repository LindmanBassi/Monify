package br.com.bassi.monify.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class RecursoNaoEncontradoException extends MonifyException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public ProblemDetail toProblemDetail() {
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        pd.setTitle("Recurso não encontrado");
        pd.setDetail(getMessage());
        return pd;
    }
}
