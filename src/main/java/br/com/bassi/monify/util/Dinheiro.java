package br.com.bassi.monify.util;

import br.com.bassi.monify.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Validação e normalização de valores monetários (no máximo 2 casas decimais).
public final class Dinheiro {

    public static final int ESCALA = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(ESCALA);
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    private static final BigDecimal CEM = new BigDecimal("100");

    private Dinheiro() {
    }

    public static BigDecimal normalizar(BigDecimal valor, String campo) {

        if (valor == null) {
            throw new RegraNegocioException(campo + " é obrigatório");
        }
        if (valor.stripTrailingZeros().scale() > ESCALA) {
            throw new RegraNegocioException(campo + " deve ter no máximo 2 casas decimais");
        }

        return valor.setScale(ESCALA);
    }

    public static BigDecimal validarPositivo(BigDecimal valor, String campo) {

        var valorNormalizado = normalizar(valor, campo);

        if (valorNormalizado.signum() <= 0) {
            throw new RegraNegocioException(campo + " deve ser maior que zero");
        }

        return valorNormalizado;
    }

    public static BigDecimal calcularPercentual(BigDecimal parte, BigDecimal total) {
        return parte.multiply(CEM).divide(total, ESCALA, ARREDONDAMENTO);
    }
}
