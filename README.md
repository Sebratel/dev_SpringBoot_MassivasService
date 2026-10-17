# dev_SpringBoot_MassivasService

Usuários afetados, massivas (Elleven/ERP/Native), histórico, GMUD, Matrix e sites de autenticação.

Parte da migração do BFF para API Gateway (`Sebratel/dev_SpringBoot_APIGateway`, `specs/API_GATEWAY.md`).
Serviço **massivas-service**, porta **8101**, atrás do `dev_SpringBoot_Gateway`.

> **Estado: esqueleto.** Sobe, expõe `/actuator/health` (público; o resto exige JWT) e tem build, Checkstyle e JaCoCo ativos, mas **a lógica ainda está no
> monólito**. As rotas abaixo são o contrato que este serviço precisa atender, idêntico ao do BFF atual (`contract/routes.json`).

## Rotas que este serviço deve atender (74)

| Método | Rota | Acesso |
|---|---|---|
| `GET` | `/api/v1/afetados` | autenticada |
| `POST` | `/api/v1/afetados` | autenticada |
| `GET` | `/api/v1/afetados/contract/{contractId}` | pública |
| `GET` | `/api/v1/afetados/pppoe/{pppoe}` | autenticada |
| `DELETE` | `/api/v1/afetados/protocol/{protocol}` | autenticada |
| `GET` | `/api/v1/afetados/protocol/{protocol}` | autenticada |
| `PATCH` | `/api/v1/afetados/protocol/{protocol}` | autenticada |
| `POST` | `/api/v1/gmud/abrir-via-api` | autenticada |
| `DELETE` | `/api/v1/gmud/encerrar-via-api` | autenticada |
| `DELETE` | `/api/v1/gmud/finalizar-via-api` | autenticada |
| `POST` | `/api/v1/gmud/open-via-api` | autenticada |
| `POST` | `/api/v1/gmud/relato-via-api` | autenticada |
| `POST` | `/api/v1/gmud/report-via-api` | autenticada |
| `GET` | `/api/v1/impacted-users` | autenticada |
| `POST` | `/api/v1/impacted-users` | autenticada |
| `GET` | `/api/v1/impacted-users/contract/{contractId}` | autenticada |
| `GET` | `/api/v1/impacted-users/pppoe/{pppoe}` | autenticada |
| `DELETE` | `/api/v1/impacted-users/protocol/{protocol}` | autenticada |
| `GET` | `/api/v1/impacted-users/protocol/{protocol}` | autenticada |
| `PATCH` | `/api/v1/impacted-users/protocol/{protocol}` | autenticada |
| `POST` | `/api/v1/massivas` | autenticada |
| `POST` | `/api/v1/massivas-eleven` | autenticada |
| `POST` | `/api/v1/massivas-eleven/abrir-protocolo-infra-via-api` | autenticada |
| `GET` | `/api/v1/massivas-eleven/estimated-end/contract/{contractNumber}` | autenticada |
| `DELETE` | `/api/v1/massivas-eleven/finalizar-chamado-via-api` | autenticada |
| `DELETE` | `/api/v1/massivas-eleven/finalize-ticket-via-api` | autenticada |
| `GET` | `/api/v1/massivas-eleven/getAllMassives` | autenticada |
| `POST` | `/api/v1/massivas-eleven/open-infra-solicitation-via-api` | autenticada |
| `GET` | `/api/v1/massivas-eleven/previsao-finalizacao/contrato/{contractNumber}` | autenticada |
| `GET` | `/api/v1/massivas-eleven/recover-via-database` | autenticada |
| `GET` | `/api/v1/massivas-eleven/recuperar-pelo-banco` | autenticada |
| `POST` | `/api/v1/massivas-eleven/salvar-massiva-via-api` | autenticada |
| `POST` | `/api/v1/massivas-eleven/save-massive-via-api` | autenticada |
| `GET` | `/api/v1/massivas-history/recover-via-database` | autenticada |
| `GET` | `/api/v1/massivas-history/recuperar-pelo-banco` | autenticada |
| `PATCH` | `/api/v1/massivas-history/{protocol}/encerrar` | autenticada |
| `PATCH` | `/api/v1/massivas-history/{protocol}/finalize` | autenticada |
| `GET` | `/api/v1/massivas-splitters/recover-via-database` | autenticada |
| `GET` | `/api/v1/massivas-splitters/recuperar-pelo-banco` | autenticada |
| `PATCH` | `/api/v1/massivas-splitters/{protocol}/encerrar` | autenticada |
| `PATCH` | `/api/v1/massivas-splitters/{protocol}/finalize` | autenticada |
| `POST` | `/api/v1/massivas/abrir-protocolo-infra-via-api` | autenticada |
| `GET` | `/api/v1/massivas/estimated-end/contract/{contractNumber}` | autenticada |
| `DELETE` | `/api/v1/massivas/finalizar-chamado-via-api` | autenticada |
| `DELETE` | `/api/v1/massivas/finalize-ticket-via-api` | autenticada |
| `GET` | `/api/v1/massivas/getAllMassives` | autenticada |
| `POST` | `/api/v1/massivas/open-infra-solicitation-via-api` | autenticada |
| `GET` | `/api/v1/massivas/previsao-finalizacao/contrato/{contractNumber}` | autenticada |
| `GET` | `/api/v1/massivas/recover-via-database` | autenticada |
| `GET` | `/api/v1/massivas/recuperar-pelo-banco` | autenticada |
| `POST` | `/api/v1/massivas/salvar-massiva-via-api` | autenticada |
| `POST` | `/api/v1/massivas/save-massive-via-api` | autenticada |
| `POST` | `/api/v1/massive-incidents` | autenticada |
| `POST` | `/api/v1/massive-incidents/abrir-protocolo-infra-via-api` | autenticada |
| `GET` | `/api/v1/massive-incidents/estimated-end/contract/{contractNumber}` | autenticada |
| `DELETE` | `/api/v1/massive-incidents/finalizar-chamado-via-api` | autenticada |
| `DELETE` | `/api/v1/massive-incidents/finalize-ticket-via-api` | autenticada |
| `GET` | `/api/v1/massive-incidents/getAllMassives` | autenticada |
| `POST` | `/api/v1/massive-incidents/open-infra-solicitation-via-api` | autenticada |
| `GET` | `/api/v1/massive-incidents/previsao-finalizacao/contrato/{contractNumber}` | autenticada |
| `GET` | `/api/v1/massive-incidents/recover-via-database` | autenticada |
| `GET` | `/api/v1/massive-incidents/recuperar-pelo-banco` | autenticada |
| `POST` | `/api/v1/massive-incidents/salvar-massiva-via-api` | autenticada |
| `POST` | `/api/v1/massive-incidents/save-massive-via-api` | autenticada |
| `GET` | `/api/v1/matrix` | pública |
| `GET` | `/api/v1/sites` | autenticada |
| `GET` | `/api/v1/sites/search` | autenticada |
| `GET` | `/api/v1/usuario-afetado` | autenticada |
| `POST` | `/api/v1/usuario-afetado` | autenticada |
| `GET` | `/api/v1/usuario-afetado/contract/{contractId}` | autenticada |
| `GET` | `/api/v1/usuario-afetado/pppoe/{pppoe}` | autenticada |
| `DELETE` | `/api/v1/usuario-afetado/protocol/{protocol}` | autenticada |
| `GET` | `/api/v1/usuario-afetado/protocol/{protocol}` | autenticada |
| `PATCH` | `/api/v1/usuario-afetado/protocol/{protocol}` | autenticada |

## Desenvolvimento

O `sebratel-java-parent` vem do GitHub Packages (o Maven exige token `read:packages` até para pacotes públicos). Antes do primeiro build, crie no `~/.m2/settings.xml` um servidor
`github` com um token `read:packages`:

```xml
<settings><servers><server>
  <id>github</id><username>SEU_USUARIO</username><password>SEU_TOKEN</password>
</server></servers></settings>
```

```bash
./mvnw verify          # testes + Checkstyle + JaCoCo (cobertura mínima 0.50, herdada do parent)
./mvnw spring-boot:run
```

Variáveis de ambiente: ver `.env.example` (nunca commite o `.env`).

## Regras do repositório
- `main` só recebe PR, com 1 aprovação e o check `ci-ok` verde. Commits no padrão Conventional Commits.
- Dependências compartilhadas (`common-web`, `common-security`, `common-elleven`) vêm de `dev_Maven_Commons`
  e entram quando houver código a compartilhar.
- Remover ou renomear rota é mudança incompatível: o contrato é o manifesto do monólito (`docs/contract/manifest.json`).

## Deploy em staging (Portainer)

Stack criada **a partir do Git**: repositório `https://github.com/Sebratel/dev_SpringBoot_MassivasService`, branch `staging`,
arquivo `docker-compose-stage.yml`. O container se chama `massivas-service-stage` e fica na rede externa `proxy`.
Sem porta publicada no host: só o gateway, na rede `proxy`, alcança este serviço.

A imagem `ghcr.io/sebratel/massivas-service:staging` é construída pelo GitHub Actions (`.github/workflows/staging-image.yml`, a cada push em `staging`) a partir
do `Dockerfile` (multi-stage, Temurin 21, usuário não-root) e publicada no GHCR; o Portainer só a baixa. Construir a imagem dentro do host
passa de 90 s, e o proxy do stage derruba a requisição de deploy. O `sebratel-java-parent` é construído da fonte pública na própria imagem, sem token.

O branch `staging` recebe o código mais atualizado: `main` → `staging` antes de cada deploy.
