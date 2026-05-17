glossario
PF - cliente Pessoa Fisica
PJ - cliente Pessoa Juridica
SVP - Seguro de Vida Prestamista
SPF - Seguro de Protecao Financeira
SPF Plus - Seguro de Protecao Financeira Plus
-----------------------------------------------------------------------------------------------------------------------------------------------

1° verificar elegibilidade da loja para ofertar o SVP para o PJ.
como atualmente tudo ja existe para a pessoa fisica, é só manter

-----
anotacoes e observacoes sobre essa etapa passada no refinamento

para pessoa juridica, vamos ofertar seguro prestamista no direct

existem 3 tipos de seguro ofertados na toyota: SPF Plus(protecao financeira plus), SPF(protecao financeira) e SVP(seguro vida. em caso de morte) e no caso do PF todos são ofertados.
No caso de PJ só exite SVP (protecao vida em caso de morte)
entao, logo se uma loja só oferta SPF Plus e SPF, por dedução lógica não ofertara seguro prestamista de PJ ja que tambem nao oferta para PF.

uma coisa é a elegibibilidade do cliente pj.
outra coisa é a loja poder ofertar o SVP (seguro de vida prestamista)

uma duvida que o Eduardo ficou de me ajudar é se caso uma loja oferte o SPF Plus ela ja oferta todos os demais tbm. 
Ou seja, se existe alguma condicao onde uma protecao credencia outras de forma automatica
-----

historia do jira
História 1 – Verificar elegibilidade da loja PJ para ofertar SVP
Como sistema
Quero verificar se a loja é elegível para ofertar Seguro Prestamista para PJ.
Para garantir que a oferta de seguro só ocorra quando permitida

![img.png](img.png)

Regras de Negócio
É Necessário verificar se a loja logada SVP (SOMENTE PROTEÇÃO VIDA) - Analisar como é o retorno do endpoint (se retorna todos os tipos de seguro elegível ou somente o maior da hierarquia - e se atender ao maior, atende aos que estão “abaixo” (SPF PLUS, SPF e SVP) @Eduardo Souza  E @Carlos Pijanowski Cartaxo

A verificação é obrigatória antes de qualquer validação de cliente.

Se a loja não for elegível, o seguro:

Não deve ser ofertado;

Não deve chamar o endpoint de elegibilidade do cliente.

Manter no mesmo ponto a identificação da loja (ao escolher a loja) - verificar local de verificação para PF.

Critérios de Aceite
✅ Dado que o cliente é PJ
✅ Quando a loja não for elegível para SVP
➡️ Então o seguro não deve ser ofertado em nenhuma etapa da simulação

✅ Dado que o cliente é PJ
✅ Quando a loja for elegível para SVP
➡️ Então o fluxo deve seguir para verificação de elegibilidade do cliente

-----------------------------------------------------------------------------------------------------------------------------------------------
2° verificar elegibilidade do cliente PJ

-----
anotacoes e observacoes sobre essa etapa passada no refinamento

-----

historia do jira
História 2 – Verificar Elegibilidade Cliente

Key details
Description

Como sistema
Quero validar a elegibilidade do cliente PJ
Para decidir se o seguro pode ser cotado

Regras de Negócio
Valor financiado inicial = Valor do bem (valor do bem, itens financiados,  serviços, seguros, conforme lógica já existente) – Valor da entrada

Chamar endpoint de elegibilidade - Chamar endpoint /seguros/v2/prestamista/juridica/calculo para verificar elegibilidade para o cliente. 
(isso é novo, atualmente a regra está na SIMU, mas para PJ deverá ser chamado o endpoint de seguros)

Valor seguro retornado pelo endoint derá ser somado ao valor financiado
SIMU deve calcular juros sobre o valor total (Valor do bem + valor seguro)
Valor final da proposta = valor financiado + valor do seguro + juros
Verifica se cliente é elegível novamente, agora considerando o valor financiado total (chamar endpoint de elegibilidade novamente OU fazer cálculo na aplicação)
Cálculo: Valor comprometido do cliente + Valor da proposta <= Limite Máximo do Produto (somente contratos com o seguro prestamista deve ser considerado no valor comprometido do cliente)

Se for menor, ofertar produto
Se for maior que o limite maximo, não ofertar produto.
Seguro deve ser removido automaticamente
Recalcular simulação sem seguro

Em caso de erro técnico ou funcional:
Seguro não deve ser ofertado
Fluxo de simulação principal deve continuar
O cliente só verá o resultado final após confirmação completa da elegibilidade e valor comprometido.
OBS IMPORTANTE: Toda inclusão/remoção/alteração de qualquer produto e serviço, INCLUINDO  SERVIÇO GEOLOCALIZAÇÃO, valorres da simulação, demandará um recalculo, conforme já é atualmente.

-----------------------------------------------------------------------------------------------------------------------------------------------
