# ranking-str-data

![Java 21](https://img.shields.io/badge/runtime-Java%2021-111827?style=for-the-badge&labelColor=111827&color=5b5ef4) ![Spark](https://img.shields.io/badge/data-Spark%204.1.2-111827?style=for-the-badge&labelColor=111827&color=5b5ef4) ![Maven](https://img.shields.io/badge/build-Maven-111827?style=for-the-badge&labelColor=111827&color=5b5ef4) ![Status](https://img.shields.io/badge/status-research%20CLI-111827?style=for-the-badge&labelColor=111827&color=5b5ef4)

[English version](README.md)

Локальный Java CLI для ранжирования Y-STR гаплотипов относительно выбранного базового гаплотипа и добавления TMRCA-метрик в CSV-файл с разделителем `;`.

| Область | Детали |
| --- | --- |
| Runtime | Java 21, Maven |
| Точка входа | `ranking.Main` |
| Артефакт | Maven `artifactId` `ranking`, итоговый jar `target/ranking.jar` |
| Data engine | Spark 4.1.2 со Scala 2.13, Hadoop 3.5.0 |
| CLI parser | JCommander 1.82 |
| Логирование | Log4j 2.26.0 |
| Тесты и QA | JUnit 6.1.0, AssertJ 3.27.7, JaCoCo 0.8.14, SpotBugs, FindSecBugs, Spotless |
| Примеры данных | `assets/DataSet.csv`, `assets/RankedData.csv` |

## Цель

Проект создан для исследовательского процесса в генетике. Он читает строки Y-STR гаплотипов, сравнивает каждую строку с единственным выбранным базовым гаплотипом, считает исследовательские метрики и записывает ранжированный CSV рядом с входным файлом.

Это локальный исследовательский инструмент, а не клиническая или диагностическая система.

## Исследовательская модель

Код реализует линейный метод Клёсова (2009a) для оценки TMRCA по Y-STR с поправкой на обратные мутации.

1. `ASD = sum((ref_i - comp_i)^2) / n`, где `n` это количество сравнимых локусов.
2. `TMRCA = averageAge * ASD / mutationRate`, результат в годах.
3. `lambda = mutationRate * T_generations`, где `T_generations = TMRCA / averageAge`.
4. `k = (lambda / 2) * (1 + exp(-lambda))`, прямая формула Клёсова для наблюдаемых шагов мутаций с поправкой на обратные мутации.

Частота мутаций по умолчанию, `0.0026` на локус на поколение, основана на Ballantyne et al. 2010 как средневзвешенное значение по 186 Y-STR маркерам. Ее можно изменить через `--mu`.

## Использование CLI

Собрать исполняемый jar:

```bash
mvn clean package
```

Запустить с обязательными флагами:

```bash
java -jar target/ranking.jar -p assets/DataSet.csv -i indexOfHaplotype -a averageAge
```

Запустить с другой частотой мутаций:

```bash
java -jar target/ranking.jar -p assets/DataSet.csv -i indexOfHaplotype -a averageAge --mu 0.0024
```

Показать справку:

```bash
java -jar target/ranking.jar -h
```

Поддерживаемые флаги:

| Флаг | Значение |
| --- | --- |
| `-p`, `--path` | Путь к входному CSV-файлу |
| `-i`, `--index` | Значение `Index` для базового гаплотипа |
| `-a`, `--age` | Средний возраст поколения для формулы TMRCA |
| `--mu` | Частота мутаций на локус на поколение, по умолчанию `0.0026` |
| `-h`, `--help` | Вывести справку CLI |

## Воспроизводимость

Ожидаемый результат локальной сборки:

```text
target/ranking.jar
```

Jar является исполняемым, потому что Maven assembly configuration указывает на `ranking.Main`, а final name проекта равен `ranking`.

Приложение запускает Spark в режиме `local[1]`, отключает Spark UI, читает CSV-строки через Spark, затем загружает все строки на driver для ранжирования.

Примеры файлов:

- Входной пример: [`assets/DataSet.csv`](assets/DataSet.csv)
- Ранжированный пример: [`assets/RankedData.csv`](assets/RankedData.csv)

Скриншоты:

![Пример 1](assets/Exanple1.png)
![Пример 2](assets/Exanple2.png)
![Пример 3](assets/Exanple3.png)

Имена файлов оставлены в том виде, в котором они сейчас находятся в репозитории.

## Входы и выходы

Требования к входным данным:

- CSV-разделитель — точка с запятой.
- Первый столбец должен называться точно `Index`.
- Значения `Index` должны быть уникальными. Дубликаты отклоняются.
- Базовый гаплотип, переданный через `-i` или `--index`, должен существовать ровно один раз.
- Значения локусов должны парситься как целые числа, если они заполнены.
- Null или пустые значения локусов пропускаются попарно при сравнении.

Поведение выхода:

- Выходной файл называется `RankedData.csv`.
- Он записывается в ту же директорию, где находится входной файл.
- Исходные входные колонки сохраняются.
- Код добавляет метрики: `TMRCA`, `Average number of actual mutations(lambda)`, `Average number of mutation steps(k)`.

## Ограничения

- Формулы являются исследовательскими допущениями и не клинически валидированы для диагностики.
- Изменения формул стоит проверять с профильным экспертом.
- Spark используется в локальном режиме `local[1]` для этого CLI-сценария.
- Ранжирование загружает все строки на driver, поэтому размер набора данных ограничен доступной памятью JVM.
- Все CSV-колонки читаются как строки перед парсингом метрик, что помогает сохранять точные значения `Index`.
- Поправка на обратные мутации предполагает низкие частоты мутаций. Точность снижается при больших значениях `lambda`.

## Ссылки

- [TMRCA](https://en.wikipedia.org/wiki/Most_recent_common_ancestor)
- [Распределение Пуассона](https://en.wikipedia.org/wiki/Poisson_distribution)
- [Y-STR гаплотипы](https://en.wikipedia.org/wiki/Haplotype#Y-DNA_haplotypes_from_genealogical_DNA_tests)
- [Клёсов А.Н. "Гаплогруппа R1a" (2009a)](https://www.scirp.org/journal/paperinformation?paperid=8736)
- [Ballantyne KN et al. "Mutability of Y-chromosomal microsatellites" (2010)](https://doi.org/10.1016/j.fsigen.2010.03.006)

## Статус

Исследовательский/учебный проект. Результаты, зависимости и runtime assumptions описаны для воспроизводимости, но репозиторий не поддерживается как packaged product.
