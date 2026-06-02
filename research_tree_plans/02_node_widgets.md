# План 2. Builder и factory для виджетов исследований

## Зачем это нужно

Сейчас нода по сути рендерится как кнопка с текстом состояния и заголовком.  
Для прототипа этого хватает, но для реального дерева исследований этого мало.

Хочется добавить:

- иконки
- стили фона
- цветовые схемы
- бейджи
- дополнительные маркеры
- индикацию состояния
- progress bar на ноде для `TIMED`
- отдельный внешний вид для закрытого / доступного / изученного / активного состояния

Если всё это встраивать прямо в `createNodeWidget`, код быстро станет очень тяжёлым.  
Поэтому лучше перейти на систему:

- описание внешнего вида
- builder описания
- factory, которая строит сам UI

---

## Целевое устройство

### Слой 1. `ResearchNodeVisualDefinition`

Описание того, как нода должна выглядеть.

Примерные поля:

- `titleVisible`
- `subtitleVisible`
- `iconVisible`
- `progressVisible`
- `backgroundColor`
- `borderColor`
- `accentColor`
- `iconTexture`
- `badgeText`
- `badgeColor`
- `shapeStyle`
- `sizePreset`
- `titleAlignment`

Это не сам `UIElement`, а описание внешнего вида.

### Слой 2. `ResearchNodeWidgetBuilder`

Fluent API для удобной сборки visual definition.

Пример:

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

Фабрика, которая:

- получает `ResearchNode`
- получает `ResearchState`
- получает `ResearchNodeVisualDefinition`
- получает progress snapshot при необходимости
- создаёт UI-виджет ноды

---

## Предлагаемые классы

### `ResearchNodeVisualDefinition`

Основная модель внешнего вида.

Минимальный состав:

- `iconPath`
- `showIcon`
- `showTitle`
- `showProgress`
- `showBadge`
- `backgroundColor`
- `accentColor`
- `borderColor`
- `badgeText`
- `badgeColor`
- `progressBarColor`
- `progressBarBackgroundColor`

### `ResearchNodeWidgetFactory`

Основной reusable класс.

Нужные методы:

- `UIElement createNodeWidget(ResearchNode node, ResearchNodeRenderContext context)`
- `void updateNodeWidget(UIElement widget, ResearchNode node, ResearchNodeRenderContext context)`

Идея: не только создавать, но и уметь обновлять существующий виджет.

### `ResearchNodeRenderContext`

Контекст рендера/состояния ноды.

Поля:

- `ResearchState state`
- `boolean visible`
- `boolean highlighted`
- `boolean revealLocked`
- `@Nullable ClientResearchProgress progress`
- `boolean selected`
- `boolean hasNewUnlockMarker`

---

## Progress bar на самой ноде

Это важное расширение для `TIMED`.

### Что нужно показывать

Если исследование `TIMED` и находится в `IN_PROGRESS`, то на самой ноде должен появляться:

- маленький progress bar
- возможно краткий percent или countdown

### Где хранить данные

Не в самой кнопке.  
Нода должна получать данные из `ResearchProgressController`.

### Как это должно работать

`ResearchTreeScreenMainScreen` или будущий node factory:

- получает progress через controller
- формирует render context
- factory обновляет progress bar

---

## Разделение логики и представления

Очень важно не смешивать:

- доменную логику исследования
- прогресс
- чистый UI

Правильная цепочка такая:

1. `ResearchNode` — логическая нода дерева
2. `ResearchProgressController` — текущее состояние исследования
3. `ResearchNodeVisualDefinition` — описание стиля
4. `ResearchNodeWidgetFactory` — создание и обновление UI

---

## Как встроить в текущий код

Сейчас `ResearchTreeScreenMainScreen` создаёт `Button` напрямую.

Лучше перейти на:

```java
protected ResearchNodeVisualDefinition buildNodeVisualDefinition(ResearchNode node, ResearchState state)
```

и

```java
protected ResearchNodeRenderContext buildNodeRenderContext(ResearchNode node)
```

после чего:

```java
nodeWidgetFactory.createNodeWidget(node, context, visualDefinition)
```

---

## Поддержка кастомных тем и групп

Этот план сразу поможет сделать разные ветки дерева визуально отличающимися.

Например:

- металлургия — тёплые жёлто-оранжевые
- farming — зелёные
- logistics — синие

И отдельно:

- locked state
- studied state
- in-progress state
- newly-unlocked state

можно будет настраивать через visual definition, а не хардкодить в одном месте.

---

## Полезные будущие расширения

### Badge-система

Например:

- `NEW`
- `!`
- `TIME`
- `TABLE`

### Icon overlay

Поверх иконки:

- замок
- галочка
- песочные часы
- вспышка нового исследования

### Animated node states

Если позже захочется:

- pulse
- glow
- progress shimmer
- opening animation

это будет проще навесить на node factory, чем на голый button-код в экране.

---

## Минимальный этап внедрения

### Этап 1

- создать `ResearchNodeVisualDefinition`
- создать `ResearchNodeRenderContext`
- создать `ResearchNodeWidgetFactory`
- перенести текущую кнопку туда без изменения внешнего вида

### Этап 2

- добавить icon support
- добавить state styling
- добавить progress bar на ноде для `TIMED`

### Этап 3

- добавить badge/overlay support
- добавить group theme presets

---

## Ожидаемый результат

После внедрения:

- логика ноды не будет смешана с её отрисовкой
- появится нормальная точка расширения для иконок и тем
- `TIMED` исследования станут информативнее прямо на дереве
- в будущем можно будет быстро менять визуальный стиль без переписывания экрана
