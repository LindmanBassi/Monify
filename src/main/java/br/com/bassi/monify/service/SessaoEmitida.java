package br.com.bassi.monify.service;

// o token de renovação nunca vai no corpo da resposta: o controller o envia em cookie httpOnly
public record SessaoEmitida(String tokenAcesso, String tokenRenovacao, long expiraEmSegundos) {
}
