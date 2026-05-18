glossario
PF - cliente Pessoa Fisica
PJ - cliente Pessoa Juridica
SVP - Seguro de Vida Prestamista
SPF - Seguro de Protecao Financeira
SPF Plus - Seguro de Protecao Financeira Plus
direct - é o nome do sistema interno
-----------------------------------------------------------------------------------------------------------------------------------------------
 
Anotações e observações sobre essa etapa passada no refinamento


  
----------------------------------------------------------------------------------------------------------------------------------------------- 

historia do jira
História 2 – Verificar Elegibilidade Cliente

Key details
para pessoa juridica, vamos ofertar seguro prestamista no direct (direct é o nome do sistema interno)

existem 3 tipos de seguro ofertados na toyota: SPF Plus, SPF e SVP. Clientes PF todos os seguros são ofertados.
No caso de PJ só existirá SVP (Seguro de Vida Prestamista - protecao vida em caso de morte)
então, logo se uma loja só oferta SPF Plus e SPF, por dedução lógica não ofertara seguro prestamista de PJ ja que também não oferta para PF.

uma coisa é a elegibibilidade do cliente pj.
outra coisa é a loja poder ofertar o SVP (seguro de vida prestamista)

Como sistema
Quero validar a elegibilidade do cliente PJ
Para decidir se o seguro pode ser cotado

Regras de Negócio
Valor financiado inicial = Valor do bem (valor do bem, itens financiados, serviços, seguros, conforme lógica já existente) – Valor da entrada

Chamar endpoint de elegibilidade - Chamar endpoint /seguros/v2/prestamista/juridica/calculo para verificar elegibilidade para o cliente. 
(isso é novo, atualmente a regra está na SIMU, mas para PJ deverá ser chamado o endpoint de seguros)

Valor seguro retornado pelo endoint derá ser somado ao valor financiado
SIMU deve calcular juros sobre o valor total (Valor do bem + valor seguro)
Valor final da proposta = valor financiado + valor do seguro + juros
Verifica se cliente é elegível novamente, agora considerando o valor financiado total (chamar endpoint de elegibilidade novamente OU fazer cálculo na aplicação)
Cálculo: Valor comprometido do cliente + Valor da proposta ≤ Limite Máximo do Produto (somente contratos com o seguro prestamista deve ser considerado no valor comprometido do cliente)

Se for menor, ofertar produto
Se for maior que o limite maximo, não ofertar produto.
Seguro deve ser removido automaticamente
Recalcular simulação sem seguro

Em caso de erro técnico ou funcional:
Seguro não deve ser ofertado
Fluxo de simulação principal deve continuar
O cliente só verá o resultado após confirmação completa da elegibilidade e valor comprometido.
OBS IMPORTANTE: Toda a inclusão/remoção/alteração de qualquer produto e serviço, INCLUINDO  SERVIÇO GEOLOCALIZAÇÃO, valorres da simulação, demandará um recalculo, conforme já é atualmente.

-----------------------------------------------------------------------------------------------------------------------------------------------
