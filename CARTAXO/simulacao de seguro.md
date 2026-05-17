# 3. Simulação do Seguro

No processo atual de simulação do produto de seguro Prestamista para pessoas físicas, o cálculo está sendo realizado por um serviço pertencente ao contexto de financiamento. No entanto, para este produto será criado um serviço de cálculo do valor do seguro pela área da corretora, de forma que, em breve, o contexto do produto de seguro Prestamista para pessoas físicas também seja migrado.

Na parte visual, considerando que hoje não é ofertado nenhum produto de seguro para pessoas jurídicas, será necessário incluí-lo, de modo que a oferta do seguro Prestamista (SVP) para PJ seja realizada da mesma forma que ocorre atualmente para PF. Deve ser contemplado as telas abaixo:

- Simulação
- Oferta
- Resumo
- Confirmação (fluxo de alteração de proposta)
- Detalhes da proposta na tela de consulta
- S-Works
- Docusing
- CCB
- CET

Caso seja identificada alguma tela onde é apresentada oferta de seguro prestamista e não conste na lista acima, deve ser também contemplada para inclusão da oferta do produto.

No domínio de seguros será desenvolvido um serviço responsável tanto pelo cálculo do valor do seguro quanto pelo retorno da elegibilidade do cliente para a contratação. Seguem abaixo mais detalhes sobre o serviço.

---

## Endpoint

- **Método:** `POST`
- **Path:** `/seguros/v2/prestamista/juridica/calculo`

---

## Request Body

> `valor-parcela` é o único parâmetro **não obrigatório**.

```json
{
  "canal-origem": "DIRECT",
  "cnpj-origem-negocio": "91919940439101",
  "cnpj-cliente": "11222333000181",
  "chave-origem": "SIMULACAO-TESTE-001",
  "valor-base-calculo": 100000.00,
  "prazo": 12,
  "valor-parcela": 8500.00,
  "flag-financia-iof": true,
  "valor-iof-operacao": 0.00
}
```

---

## Responses

### ✅ Sucesso — `200 OK`

```json
{
  "response-code": "SEGPJ0000",
  "message": "SPJ ELEGIVEL VALOR CALCULADO COM SUCESSO.",
  "timestamp": "2026-05-17T17:47:29.650773966-03:00",
  "calculation-code": "6a0a2961ea04e4f1ab91ef2d",
  "data": {
    "tipo-cobertura": "VIDA_PRESTAMISTA-PJ",
    "cnpj-seguradora": "03546261000108",
    "valor-seguro": 6000.00,
    "valor-comprometido-cliente": 0.00,
    "flag-financia-iof": true,
    "configuracao": {
      "item-id": 43,
      "descricao": "Item mock comissao PJ loja 75 3",
      "fator-prestamista": 0.0600000000,
      "forma-calculo-descricao": "VALOR-BASE-CALCULO * FATOR-SEGURO",
      "forma-calculo-valor": "100000.00 * 0.0600000000",
      "tempo-maximo-contrato": 12,
      "valor-maximo-financiamento-contrato": 333000.00,
      "percentual-iof": 7.3800,
      "percentual-pro-labore": 5.0000,
      "fator-morte": 0.0300000000,
      "fator-invalidez": 0.0200000000,
      "fator-desemprego": 0.0100000000,
      "valor-maximo-por-cliente": 999000.00
    }
  }
}
```

### ❌ Erro — `400 BAD REQUEST`

```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Bad request",
  "timestamp": "2026-05-17T17:48:17.134059473",
  "detail-message": "CNPJ DA ORIGEM NEGOCIO INVALIDO.",
  "uri-patch": "/seguros",
  "response-code": "SEGPJ1003"
}
```

---

## Descrição dos Campos da Response

| Campo | Descrição |
|---|---|
| `response-code` | Código de resposta de direcionamento de fluxo |
| `message` | Mensagem relacionada ao código de resposta |
| `calculation-code` | Código de registro de cotação do valor de seguro |
| `timestamp` | Data e hora da resposta |
| `data.tipo-cobertura` | Tipo da cobertura de prestamista |
| `data.cnpj-seguradora` | CNPJ da seguradora do seguro prestamista |
| `data.valor-seguro` | Valor de seguro calculado |
| `data.valor-comprometido-cliente` | Valor comprometido do cliente |
| `data.flag-financia-iof` | Flag que indica se o IOF está sendo incluído no valor de seguro |
| `data.configuracao.item-id` | ID sequencial do item de configuração aplicado no cálculo |
| `data.configuracao.descricao` | Descrição do item de configuração |
| `data.configuracao.fator-prestamista` | Fator de seguro |
| `data.configuracao.forma-calculo-descricao` | Descrição da forma do cálculo |
| `data.configuracao.forma-calculo-valor` | Valor da forma do cálculo |
| `data.configuracao.tempo-maximo-contrato` | Número de meses máximo para contrato |
| `data.configuracao.valor-maximo-financiamento-contrato` | Valor de limite máximo do financiamento no contrato |
| `data.configuracao.percentual-iof` | Valor percentual de IOF |
| `data.configuracao.percentual-pro-labore` | Valor percentual de pró-labore |
| `data.configuracao.fator-morte` | Fator de seguro para morte |
| `data.configuracao.fator-invalidez` | Fator de seguro para invalidez |
| `data.configuracao.fator-desemprego` | Fator de seguro para desemprego |
| `data.configuracao.valor-maximo-por-cliente` | Valor de limite máximo de contratos para o cliente |

---

## Códigos de Resposta

| Código | Descrição |
|---|---|
| `SEGPJ0000` | SPJ ELEGIVEL — VALOR CALCULADO COM SUCESSO |
| `SEGPJ1002` | CNPJ DA ORIGEM NEGOCIO NÃO INFORMADO |
| `SEGPJ1003` | CNPJ DA ORIGEM NEGOCIO INVÁLIDO |
| `SEGPJ1004` | CNPJ DO PROPONENTE NÃO INFORMADO |
| `SEGPJ1005` | CNPJ DO PROPONENTE INVÁLIDO |
| `SEGPJ1006` | VALOR BASE CÁLCULO DEVE SER MAIOR QUE `(X)` — parâmetro do produto |
| `SEGPJ1007` | VALOR BASE CALCULO NÃO INFORMADO |
| `SEGPJ1008` | CANAL DE ORIGEM NÃO INFORMADO |
| `SEGPJ2009` | VALOR BASE CÁLCULO + VALOR COMPROMETIDO DO CLIENTE ULTRAPASSA VALOR MÁXIMO DO PRODUTO |
| `SEGPJ2010` | SPJ ELEGIVEL — VALOR CALCULADO COM SUCESSO. ⚠️ ATENÇÃO: REALIZAR CHECKLIST |
