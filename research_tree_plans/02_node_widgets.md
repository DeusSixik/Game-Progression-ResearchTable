# План 2. Builder и factory для виджетов исследований

## Статус

Базовый план уже реализован.

Что уже есть в проекте:

- `ResearchNodeVisualDefinition` как отдельная модель внешнего вида ноды
- `ResearchNodeRenderContext` как отдельный контекст рендера/состояния
- `ResearchNodeWidgetFactory` с разделением на `createNodeWidget(...)` и `updateNodeWidget(...)`
- reusable-реализация `DefaultResearchNodeWidgetFactory`
- сборка visual definition и render context внутри `ResearchTreeScreenMainScreen`
- progress bar на самой ноде для `TIMED`
- group theme presets и branch-specific styling
- отдельный debug/tutorial showcase для кастомных node widgets

Итог: цель плана достигнута. Ниже план переписан в актуальном виде, чтобы он отражал уже внедренную архитектуру и оставшиеся optional-расширения.

---

## Зачем это было нужно

Раньше нода по сути рендерилась как простая кнопка с текстом состояния и заголовком.
Для прототипа этого хватало, но для нормального дерева исследований было нужно разделить:

- доменную логику исследований
- описание внешнего вида ноды
- фабрику, которая строит UI

Также была цель добавить:

- иконки
- стили фона
- цветовые схемы по группам
- badge-элементы
- индикаторы состояния
- progress bar на ноде для `TIMED`
- разные визуальные состояния для locked / available / studied / selected / in-progress

---

## Итоговая архитектура

### Слой 1. `ResearchNodeVisualDefinition`

Отдельная модель, которая описывает внешний вид ноды, а не сам `UIElement`.

Уже используется для таких параметров:

- `titleVisible`
- `subtitleVisible`
- `iconVisible`
- `progressVisible`
- `badgeVisible`
- `backgroundColor`
- `borderColor`
- `accentColor`
- `iconPath`
- `badgeText`
- `badgeColor`
- `progressBarColor`
- `progressBarBackgroundColor`
- `shapeStyle`
- `sizePreset`
- `titleAlignment`

`ResearchNodeVisualDefinition` уже стал основной reusable-точкой настройки node visuals.

### Слой 2. Builder visual definition

Изначально в плане был отдельный `ResearchNodeWidgetBuilder`.

На практике его роль сейчас выполняют:

- builder внутри `ResearchNodeVisualDefinition`
- `ResearchNodeTheme`
- `ResearchNodeGroupThemeResolver`
- `ResearchNodeVisualResolver`

То есть fluent API для сборки внешнего вида уже есть, просто оно реализовано не отдельным классом `ResearchNodeWidgetBuilder`, а через связку visual-definition builder + theme/resolver layer.

Пример направления использования:

```java
ResearchNodeVisualDefinition visual = ResearchNodeVisualDefinition.builder()
        .icon("textures/research/steam.png")
        .showTitle(true)
        .showProgress(true)
        .backgroundColor(0xFF223344)
        .accentColor(0xFF67B7FF)
        .badgeText("NEW")
        .build();
```

### Слой 3. `ResearchNodeWidgetFactory`

Фабрика вынесена в отдельный reusable-контракт.

Актуальные методы:

- `UIElement createNodeWidget(ResearchNode node, ResearchNodeRenderContext context, ResearchNodeVisualDefinition visualDefinition, Runnable onClick)`
- `void updateNodeWidget(UIElement widget, ResearchNode node, ResearchNodeRenderContext context, ResearchNodeVisualDefinition visualDefinition)`

Это лучше исходного плана, потому что фабрика теперь сразу получает:

- саму ноду
- render context
- resolved visual definition
- callback на выбор ноды

И может как создавать новый виджет, так и дешево обновлять уже существующий.

---

## `ResearchNodeRenderContext`

Контекст рендера/состояния ноды уже вынесен в отдельный класс.

Он хранит:

- `ResearchState state`
- `boolean visible`
- `boolean highlighted`
- `boolean revealLocked`
- `@Nullable ClientResearchProgress progress`
- `boolean selected`
- `boolean hasNewUnlockMarker`
- `long nowMs`

Это позволяет не хранить transient UI-state внутри самой кнопки и не смешивать визуализацию с логикой исследований.

---

## Progress bar на самой ноде

Пункт реализован.

Если исследование:

- имеет тип `TIMED`
- находится в состоянии `IN_PROGRESS`
- имеет client progress snapshot

то factory показывает progress bar прямо на ноде.

Важно, что данные берутся не из самой кнопки, а через controller/context chain:

1. `ResearchTreeScreenMainScreen` получает progress из progress controller
2. формирует `ResearchNodeRenderContext`
3. строит `ResearchNodeVisualDefinition`
4. передает все это в factory
5. factory обновляет progress bar

Это как раз и было одной из главных целей плана.

---

## Разделение логики и представления

Целевое разделение достигнуто.

Текущая цепочка выглядит так:

1. `ResearchNode` - логическая нода дерева
2. `ResearchProgressController` / client progress manager - текущее состояние исследования
3. `ResearchNodeRenderContext` - моментальный render-state
4. `ResearchNodeVisualDefinition` - описание внешнего вида
5. `ResearchNodeWidgetFactory` - создание и обновление UI

За счет этого логика ноды больше не смешана с ее отрисовкой в экране.

---

## Как это встроено в экран

Этот шаг тоже уже завершен.

`ResearchTreeScreenMainScreen` больше не строит ноду как жестко зашитую кнопку напрямую.
Теперь он:

```java
protected ResearchNodeRenderContext buildNodeRenderContext(ResearchNode node)
```

и

```java
protected ResearchNodeVisualDefinition buildNodeVisualDefinition(ResearchNode node, ResearchNodeRenderContext context)
```

после чего передает данные в factory:

```java
nodeWidgetFactory.createNodeWidget(node, context, visualDefinition, onClick)
```

и позже:

```java
nodeWidgetFactory.updateNodeWidget(widget, node, context, visualDefinition)
```

---

## Поддержка кастомных тем и групп

Пункт реализован.

Уже есть:

- branch/group presets
- разные цветовые схемы для веток
- badge text по темам
- accent colors
- progress colors
- title alignment presets

Примеры групповых тем уже используются для:

- metallurgy
- farming
- logistics
- root

Отдельно debug showcase доказывает, что поверх той же data model можно строить полностью разные layouts node widgets.

---

## Что уже покрыто из этапов внедрения

### Этап 1 - выполнен

- создан `ResearchNodeVisualDefinition`
- создан `ResearchNodeRenderContext`
- создан `ResearchNodeWidgetFactory`
- базовая кнопка вынесена в reusable factory-слой

### Этап 2 - выполнен

- добавлена icon support
- добавлен state styling
- добавлен progress bar на ноде для `TIMED`

### Этап 3 - в основном выполнен

- добавлен badge support
- добавлены group theme presets
- добавлен debug showcase кастомных композиций node widgets

---

## Оставшиеся optional-расширения

Эти пункты не мешают считать план завершенным, но их можно развивать отдельно.

### 1. Реальный `hasNewUnlockMarker`

Поле уже есть в `ResearchNodeRenderContext`, и visual resolver умеет на него реагировать,
но сейчас оно еще не заведено как полноценный live-flow от состояния прогрессии.

### 2. Полноценный icon overlay API

Пока есть badge/progress/theme support, но отдельного общего overlay-слоя для:

- замка
- галочки
- песочных часов
- маркера нового исследования

еще нет как завершенного reusable API.

### 3. Обобщенные animated node states

В проекте уже есть анимации появления/раскрытия, но это пока не оформлено как отдельный универсальный слой именно для node widgets.
В будущем сюда можно вынести:

- pulse
- glow
- progress shimmer
- reusable opening animation hooks

### 4. Еще более высокий уровень builder API

Если позже понадобится еще более удобный authoring API для моддеров, можно поверх текущей системы добавить отдельный high-level builder для карточек/слоев/layout presets.
Но для текущих задач существующей архитектуры уже достаточно.

---

## Итог

План считается выполненным.

После внедрения:

- логика ноды больше не смешана с ее отрисовкой
- появилась нормальная точка расширения для icon/theme/badge/progress/state styling
- `TIMED` исследования стали информативнее прямо на дереве
- стало возможно быстро менять визуальный стиль нод без переписывания экрана
- debug/showcase слой подтверждает, что система подходит не только для одной жесткой кнопки, а для разных композиций и стилей
