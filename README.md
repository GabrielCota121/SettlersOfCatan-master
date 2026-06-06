## Como Executar os Testes e Ver a Cobertura (JaCoCo)

Rodar todos os testes:
```bash
mvn clean test
```

Após rodar mvn clean test, o relatório é gerado automaticamente em:

`target/site/jacoco/index.html`

Abra esse arquivo no navegador para visualizar a cobertura de testes por classe e método.
