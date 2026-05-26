# Bugs Conhecidos

## Áudio

- Sons de passos podem tocar durante pulos devido à instabilidade na detecção de chão.
- O timing dos passos atualmente depende apenas do input de movimento.
- O sistema de física/colisão ainda precisa de estabilização do estado grounded.

## Física

- Pequeno jitter de colisão nas bordas dos blocos.
- A detecção vertical de chão pode oscilar em transições de blocos/declives.

## Renderização

- A ordenação de transparência da água ainda não está totalmente correta.

## Mobs

- Mobs não são salvos ao fechar o jogo.
- Não é possível acertar mobs corretamente quando existem blocos atrás deles.

## Mundo

- O sistema atual pode gerar cavernas excessivamente grandes/infindas.
- Veios de minérios ainda podem gerar em tamanhos exagerados.