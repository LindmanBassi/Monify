param(
    [Parameter(Mandatory = $true)][string]$Usuario,
    [Parameter(Mandatory = $true)][string]$Versao
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

$imagens = @(
    @{ Nome = 'monify-api'; Contexto = $raiz },
    @{ Nome = 'monify-web'; Contexto = (Join-Path $raiz 'frontend') }
)

foreach ($imagem in $imagens) {
    $base = "$Usuario/$($imagem.Nome)"
    docker build --platform linux/amd64 -t "${base}:$Versao" -t "${base}:latest" $imagem.Contexto
    if ($LASTEXITCODE -ne 0) { throw "falha ao gerar $base" }
    foreach ($tag in @($Versao, 'latest')) {
        docker push "${base}:$tag"
        if ($LASTEXITCODE -ne 0) { throw "falha ao enviar ${base}:$tag" }
    }
}
