# План 1. `ResearchInfoPanel` и builder-система для информационной панели

## Статус

Базовый план уже реализован.

Что уже есть в проекте:

- `ResearchInfoContent` как отдельная view-model для панели
- builder внутри `ResearchInfoContent`
- `ResearchInfoSection` и `ResearchInfoEntry` для секций и строк
- `ResearchInfoContentFactory` для стандартной сборки metadata / conditions / unlocks / timed progress / research button
- `ResearchInfoPanelWidget` как базовый контракт панели
- `AdaptiveResearchInfoPanelWidget` как удобная база для адаптивных panel widgets
- готовая reusable-панель `ResearchInfoPanel`
- кастомный пример `DebugCustomInfoPanel`
- `ResearchInfoPanelContext` для close / research / jump actions
- интеграция в `ResearchTreeScreenMainScreen`
- scrollable content area для больших наборов секций
- jump buttons с sanitizing видимости целей

Итог: цель плана достигнута. Ниже план переписан в актуальном виде, чтобы он отражал уже внедренную архитектуру и оставшиеся optional-расширения.

---

## Зачем это было нужно

Изначально details panel должна была показывать не только базовую информацию:

- `title`
- `description`
- `state`
- `study mode`
- текущий прогресс исследования

но и расширяемые проектные блоки:

- условия
- награды
- открываемые исследования
- дополнительные кастомные секции
- отдельное поведение для `TABLE`
- кнопки навигации по связанным исследованиям

Если держать все это прямо внутри `ResearchTreeScreenMainScreen`, экран быстро превращается в перегруженный UI-класс.

Поэтому основная цель была такой:

- отделить данные панели от ее рендера
- отделить reusable panel API от конкретного визуального стиля
- сделать удобный builder/factory слой для наполнения панели

---

## Итоговая архитектура

### Слой 1. `ResearchInfoContent`

Это отдельная view-model для панели, а не сам UI.

Она уже хранит:

- базовые заголовки и тексты
- timed progress состояние
- текст и видимость research button
- список секций `ResearchInfoSection`

Смысл этого слоя в том, что экран и gameplay-логика больше не управляют напрямую отдельными label/button элементами панели.

### Слой 2. Builder content-модели

Для `ResearchInfoContent` уже есть builder API.

Также поверх него существует более высокий уровень сборки:

- `ResearchInfoContentFactory`
- `ResearchInfoPresentationRules`
- sugar API внутри `ResearchInfoSection.Builder`

То есть фактический builder-слой получился даже удобнее исходного плана, потому что:

- низкоуровневую view-model можно собрать вручную
- стандартный контент можно получить через factory
- поверх factory можно дозаполнить контент своими секциями

Пример направления использования:

```java
ResearchInfoContent content = ResearchInfoContent.builder()
        .title("Steam Power")
        .description("Pressure, heat and primitive power systems.")
        .timedProgress("Progress: 35%", 0.35f, 0xFF67B7FF)
        .researchButton("Start Research", true)
        .section("Requirements", section -> section
                .conditionText("Open Metallurgy", true)
                .conditionText("Build Alloy Furnace", false))
        .build();
```

### Слой 3. `ResearchInfoPanelWidget`

UI панели теперь живет за отдельным контрактом:

- `ResearchInfoPanelWidget` - базовый абстрактный виджет панели
- `AdaptiveResearchInfoPanelWidget` - база для адаптивных и скроллируемых реализаций
- `ResearchInfoPanel` - стандартная reusable-реализация
- `DebugCustomInfoPanel` - пример кастомного визуального стиля

Это значит, что логика наполнения панели и визуальный стиль панели больше не сцеплены жестко.

---

## `ResearchInfoContent`

Этот слой уже используется как основной контейнер данных панели.

Сейчас он отвечает за:

- title/description/metadata
- timed progress visibility, text, fill
- research button visibility и text
- список секций

Важно, что модель уже не привязана к конкретному layout панели.

---

## `ResearchInfoSection` и `ResearchInfoEntry`

Изначально в плане были отдельные view-data типы вроде condition/reward/tag/extra section.

В реальной реализации это оформилось через более гибкую систему:

- `ResearchInfoSection`
- `ResearchInfoEntry`

Это оказалось удобнее, потому что одна и та же panel API теперь поддерживает:

- plain text entries
- condition entries
- reward entries
- icon + text rows
- item/ingredient-backed rows
- tooltip rows
- jump buttons к другим исследованиям

Особенно полезны sugar-методы в `ResearchInfoSection.Builder`, например:

- `infoText(...)`
- `conditionText(...)`
- `conditionItem(...)`
- `conditionResearchVisibleOnly(...)`
- `rewardItem(...)`
- `rewardIngredient(...)`
- `rewardResearchVisibleOnly(...)`
- `rewardItemId(...)`

То есть вместо большого числа узкоспециализированных DTO получилась более универсальная секционная система.

---

## `ResearchInfoContentFactory`

Этот слой уже закрывает значительную часть стандартного поведения панели.

Он умеет собирать:

- metadata section
- conditions section
- unlocks/rewards section
- timed progress block
- research button

За счет этого `ResearchTreeScreenMainScreen` не обязан каждый раз руками собирать типовой panel content.

Практический сценарий сейчас такой:

1. экран вызывает `createStandardInfoContentBuilder(node, state)`
2. factory наполняет стандартные части панели
3. subclass может поверх этого добавить свои секции
4. панель получает уже готовый `ResearchInfoContent`

---

## `ResearchInfoPanelContext`

Контекст действий панели уже вынесен в отдельный класс.

Он инкапсулирует:

- `onClose`
- `onResearch`
- `onResearchJump`

Благодаря этому panel widget не должен знать ничего о screen internals и работает только через понятные точки входа:

- `closePanel()`
- `startResearch()`
- `jumpToResearch(...)`

---

## Что уже делает стандартная панель

Стандартная `ResearchInfoPanel` уже поддерживает:

- title
- description
- timed progress label
- timed progress bar
- scrollable sections container
- research button
- close button
- item/icon displays
- tooltips
- optional jump buttons

Также она уже решает важную практическую проблему: длинный content уходит в `ScrollerView`, а не ломает layout экрана.

---

## Кастомизация панели

План на кастомную панель тоже уже реализован.

Сейчас можно:

- оставить стандартную `ResearchInfoPanel`
- унаследоваться от `AdaptiveResearchInfoPanelWidget`
- сделать полностью свою панель через `createInfoPanelWidget(...)`

Это уже показано на `DebugCustomInfoPanel`.

Отдельно важно, что панель поддерживает lifecycle hooks анимации:

- `onOpenAnimationStart()`
- `onOpenAnimationEnd()`
- `onCloseAnimationStart()`
- `onCloseAnimationEnd()`

То есть API панели уже учитывает не только layout, но и анимационный жизненный цикл.

---

## Как это встроено в экран

Этот шаг тоже уже завершен.

`ResearchTreeScreenMainScreen` теперь отвечает только за orchestration:

- знает выбранную ноду
- знает ее runtime state
- собирает `ResearchInfoContent`
- передает его в panel widget
- санитизирует jump actions через правила видимости

Ключевые точки расширения:

```java
protected ResearchInfoPanelWidget createInfoPanelWidget(ResearchInfoPanelContext context)
```

```java
protected ResearchInfoContent buildInfoContent(ResearchNode node, ResearchState state)
```

```java
protected final ResearchInfoContentFactory.Builder createStandardInfoContentBuilder(ResearchNode node, ResearchState state)
```

Это и есть та архитектура, к которой вел исходный план.

---

## Поддержка разных study modes

Базовая поддержка study modes уже есть.

### `INSTANT`

Панель может показать:

- состояние
- обычную кнопку исследования

### `TIMED`

Панель может показать:

- описание
- progress bar
- progress text
- корректную кнопку по текущему runtime state

### `TABLE`

Панель может показать:

- описание
- кнопку запуска table flow
- project-specific секции и подсказки

Само table-поведение затем уводится в отдельный overlay / screen flow, а не запихивается внутрь panel layout.

---

## Что уже покрыто из этапов внедрения

### Этап 1 - выполнен

- создан `ResearchInfoContent`
- создан builder для `ResearchInfoContent`
- создан `ResearchInfoPanel`
- базовые данные больше не рендерятся напрямую экраном через набор label'ов

### Этап 2 - выполнен

- добавлены conditions
- добавлены rewards / unlock rows
- добавлены кастомные секции
- добавлены item/ingredient/icon-backed entries

### Этап 3 - выполнен в основной части

- появился стандартный factory/presentation слой
- появились jump buttons и jump sanitizing
- появилась возможность полностью кастомных panel widgets
- появилась scrollable секционная структура

---

## Оставшиеся optional-расширения

Эти пункты не мешают считать план завершенным, но их можно развивать отдельно.

### 1. Еще более высокий уровень builder для авторов контента

Текущий API уже удобный, но позже можно добавить еще более декоративный builder-слой, который будет собирать готовые пресеты секций вроде:

- `standardRewardsSection(...)`
- `standardRequirementsSection(...)`
- `researchJumpRow(...)`
- `recipeUnlocksSection(...)`

### 2. Отдельная tag/chip система

Сейчас похожие вещи можно делать через entries и секции, но если понадобится компактный верхний ряд тегов/чипов, это можно выделить в отдельный reusable слой.

### 3. Более богатая table-specific панель

Сейчас table flow уже отделен от панели концептуально, но при желании можно сделать специальные `TABLE` presets для info content, чтобы еще проще описывать исследование перед открытием table overlay.

### 4. Готовые визуальные пресеты panel widgets

Сейчас есть standard panel и debug custom panel.
Позже можно добавить отдельные theme presets или panel factory для быстрого переключения между стилями.

---

## Итог

План считается выполненным.

После внедрения:

- `ResearchTreeScreenMainScreen` стал заметно чище
- данные панели отделены от ее рендера
- стандартный info content стало удобно собирать и расширять
- панель стала пригодна для больших объемов контента за счет scrollable sections
- кастомный UI панели теперь можно делать без переписывания основной логики экрана
- система уже подходит не только для одного fixed layout, а для разных визуальных стилей и разных типов исследований
