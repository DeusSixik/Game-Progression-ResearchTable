package dev.sixik.gprt.impl.client.research_screen.demo;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo-screen для исследования возможностей LDLib {@link GraphView}.
 * <p>
 * Навигация по классу:
 * 1. {@link Main} - внешний root-контейнер и overlay-панель поверх графа. <br>
 * 2. {@link ResearchNode} - описание одной research-ноды и ее world-координат. <br>
 * 3. {@link ResearchLink} - описание связи между двумя нодами. <br>
 * 4. Конструктор {@link #GraphViewDemo()} - базовая настройка GraphView и подписка на события. <br>
 * 5. {@link #seedExampleNodes()} / {@link #seedExampleLinks()} - наполнение демонстрационными данными. <br>
 * 6. {@link #addResearchNode(String, String, float, float, float, float)} - пример свободного размещения элементов. <br>
 * 7. {@link #centerCameraOn(String)}, {@link #focusCameraOnMouse(float, float)},
 * {@link #moveCameraToWorld(float, float)}, {@link #centerCameraOn(float, float)} - работа с камерой. <br>
 * 8. {@link #drawBackgroundAdditional(GUIContext)} / {@link #drawResearchLinks(GUIContext)} - кастомный рендер между элементами. <br>
 * 9. {@link #syncCameraTransform()} - ручная синхронизация transform у contentRoot. <br>
 * <p>
 * Что здесь демонстрируется: <br>
 * - GraphView как world-space canvas, где дочерние элементы лежат в общей системе координат. <br>
 * - Абсолютное позиционирование нод через ABSOLUTE + left/top. <br>
 * - Перемещение камеры к world-точке под мышью. <br>
 * - Рендер связей "под" виджетами, но "внутри" того же canvas. <br>
 * - Автоматический fitToChildren после первого layout pass. <br>
 */
public class GraphViewDemo extends GraphView {

    /**
     * Внешний корневой контейнер экрана.
     * <p>
     * Зачем он нужен:
     * - сам {@link GraphViewDemo} отвечает только за canvas с нодами;
     * - поверх него удобно положить overlay-панель с кнопками и подсказками;
     * - из-за абсолютного позиционирования overlay не зависит от зума/пана GraphView.
     */
    private static class Main extends UIElement {
        public Main() {
            var graph = new GraphViewDemo();

            // Root растягиваем на весь экран, чтобы GraphView занял всю доступную область.
            layout(layout -> layout.widthPercent(100).heightPercent(100));

            // Фон здесь задается именно root-контейнеру, а не только GraphView,
            // чтобы экран имел стабильный базовый цвет даже вокруг внутреннего canvas.
            style(style -> style.backgroundTexture(new ColorRectTexture(0xFF0E1116)));

            // Порядок детей важен:
            // 1) сначала сам graph,
            // 2) затем overlay-панель поверх него.
            addChildren(graph, createOverlay(graph));
        }

        private static UIElement createOverlay(GraphViewDemo graph) {
            // Overlay-панель - обычный UIElement, который НЕ находится в contentRoot GraphView.
            // Значит:
            // - она не скроллится,
            // - не зумится,
            // - всегда остается закрепленной в углу экрана.
            var panel = new UIElement()
                    .layout(layout -> layout
                            // Панель позиционируем абсолютно относительно root-контейнера.
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(8)
                            .top(8)
                            .width(255)
                            // Padding задает внутренние отступы панели.
                            .paddingAll(6)
                            // Gap добавляет расстояние между дочерними элементами.
                            .gapAll(4)
                    )
                    .style(style -> style.backgroundTexture(new ColorRectTexture(0xCC1A2330)));

            // Контейнер для кнопок управления камерой.
            var actions = new UIElement()
                    .layout(layout -> layout.widthPercent(100).gapAll(4));

            actions.addChildren(
                    // Встроенный GraphView helper: уместить все ноды на экране с отступом.
                    new Button().setText("Fit nodes").setOnClick(event -> graph.fitToChildren(80f, 0.35f)),

                    // Центрируем камеру по ноде с id "root".
                    new Button().setText("Center root").setOnClick(event -> graph.centerCameraOn("root")),

                    // Пример "сырого" перемещения камеры в world-координаты (0, 0).
                    new Button().setText("Origin").setOnClick(event -> graph.moveCameraToWorld(0f, 0f))
            );

            panel.addChildren(
                    new Label().setText(Component.literal("GraphView example")),
                    new Label().setText(Component.literal("LMB drag / wheel zoom / RMB center camera on mouse")),
                    actions
            );

            return panel;
        }
    }

    /**
     * Описание одной research-ноды.
     * <p>
     * Почему здесь храним и координаты, и ссылку на widget:
     * - координаты нужны для вычисления линий и фокуса камеры;
     * - widget пригодится позже, если захочешь менять стиль, состояние, tooltip, drag logic.
     * <p>
     * Важно: x/y/width/height здесь трактуются как координаты в world-space GraphView,
     * а не как экранные пиксели после зума.
     */
    private record ResearchNode(String id, float x, float y, float width, float height, Button widget) {

        private float centerX() {
            // Центр ноды по X в world-space.
            return x + width / 2f;
        }

        private float centerY() {
            // Центр ноды по Y в world-space.
            return y + height / 2f;
        }
    }

    /**
     * Простейшее описание ребра графа:
     * - fromId/toId говорят, какие ноды соединяем;
     * - startColor/endColor дают градиент вдоль линии.
     */
    private record ResearchLink(String fromId, String toId, int startColor, int endColor) {
    }

    // Список всех нод на текущем исследовательском полотне.
    private final List<ResearchNode> nodes = new ArrayList<>();

    // Список визуальных связей между нодами.
    private final List<ResearchLink> links = new ArrayList<>();

    // Нужен, чтобы fitToChildren выполнился только один раз после первого реального layout.
    private boolean fittedOnce;

    public static UIElement createView() {
        // Снаружи удобно отдавать уже готовый root, не раскрывая внутреннюю структуру экрана.
        return new Main();
    }

    private GraphViewDemo() {
        // Сам GraphView растягиваем на всю доступную область root-контейнера.
        layout(layout -> layout.widthPercent(100).heightPercent(100));

        // Фон именно canvas-области GraphView.
        style(style -> style.backgroundTexture(new ColorRectTexture(0xFF11161C)));

        // Базовая настройка поведения GraphView.
        // allowPan / allowZoom - включают встроенные механики библиотеки.
        // minScale / maxScale - ограничивают зум.
        // gridSize - шаг фоновой сетки в world-space.
        graphViewStyle(style -> style
                .allowPan(true)
                .allowZoom(true)
                .minScale(0.25f)
                .maxScale(2.5f)
                .gridSize(48f)
        );

        seedExampleNodes();
        seedExampleLinks();

        // Дополнительное пользовательское управление:
        // ПКМ переносит камеру так, чтобы точка под курсором оказалась по центру экрана.
        //
        // Capture-phase (true) здесь полезен тем, что мы перехватываем событие раньше
        // возможных дочерних элементов и можем решить, надо ли остановить всплытие.
        addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 1 && isMouseOverContent(event.x, event.y)) {
                focusCameraOnMouse(event.x, event.y);
                event.stopPropagation();
            }
        }, true);

        // GraphView нельзя корректно "fit"-нуть в конструкторе сразу:
        // до первого layout у элемента еще нет настоящих размеров content area.
        //
        // Поэтому ждем LAYOUT_CHANGED, проверяем что размеры уже появились,
        // и только тогда один раз делаем fitToChildren.
        addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            if (!fittedOnce && getContentWidth() > 0 && getContentHeight() > 0) {
                fittedOnce = true;
                fitToChildren(80f, 0.35f);
            }
        });
    }

    private void seedExampleNodes() {
        // Демонстрационные ноды расположены в произвольных world-координатах.
        // Именно это и удобно в GraphView: тебе не нужно подстраиваться под сетку layout,
        // можно раскладывать дерево исследований как угодно.
        addResearchNode("root", "Primitive Tools", 20, 20, 120, 34);
        addResearchNode("copper", "Copper Working", 220, 0, 140, 34);
        addResearchNode("alloy", "Alloy Smelting", 420, 90, 140, 34);
        addResearchNode("alloy_1", "Alloy 1", 420, 0, 140, 34);
        addResearchNode("alloy_2", "Alloy 2", 420, -90, 140, 34);
        addResearchNode("steam", "Steam Power", 650, 60, 130, 34);
        addResearchNode("chem", "Chemistry", 440, 240, 130, 34);
        addResearchNode("automation", "Automation", 700, 250, 150, 34);
    }

    private void seedExampleLinks() {
        // Здесь просто описываем логические связи.
        // Сам рендер этих связей происходит отдельно, в drawResearchLinks(...).
        links.add(new ResearchLink("root", "copper", 0xFF87D4FF, 0xFF4EB4E5));
        links.add(new ResearchLink("copper", "alloy", 0xFF89F0B7, 0xFF2FC97A));
        links.add(new ResearchLink("copper", "alloy_1", 0xFF89F0B7, 0xFF2FC97A));
        links.add(new ResearchLink("copper", "alloy_2", 0xFF89F0B7, 0xFF2FC97A));
        links.add(new ResearchLink("alloy", "steam", 0xFFFFD37A, 0xFFFF9E3D));
        links.add(new ResearchLink("alloy", "chem", 0xFFCFA5FF, 0xFF9B6BFF));
        links.add(new ResearchLink("chem", "automation", 0xFFFE8B8B, 0xFFE55151));
        links.add(new ResearchLink("steam", "automation", 0xFFFFD37A, 0xFFE55151));
    }

    private void addResearchNode(String id, String title, float x, float y, float width, float height) {
        // Button здесь выступает в роли примитивного research-widget.
        // Позже ты легко сможешь заменить его на собственный ResearchWidget.
        var nodeButton = new Button();
        nodeButton.setText(title);

        // По клику на ноду центрируем камеру на ее центре.
        nodeButton.setOnClick(event -> centerCameraOn(id));

        // Ключевой момент GraphView:
        // дочерние элементы contentRoot можно свободно расставлять через ABSOLUTE.
        //
        // x/y - это координаты в world-space canvas.
        // Они не обязаны совпадать с экранными координатами и будут потом
        // автоматически преобразованы через zoom + pan.
        nodeButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(x)
                .top(y)
                .width(width)
                .height(height)
        );

        // Добавляем ноду именно в contentRoot GraphView,
        // чтобы на нее действовали pan/zoom трансформации.
        addContentChild(nodeButton);

        // Дублируем геометрию в модели данных, чтобы удобно рисовать связи и искать центры.
        nodes.add(new ResearchNode(id, x, y, width, height, nodeButton));
    }

    public void centerCameraOn(String nodeId) {
        // Удобная перегрузка: пользователь зовет по id,
        // а дальше мы переводим это в центр world-space ноды.
        var node = findNode(nodeId);
        if (node != null) {
            centerCameraOn(node.centerX(), node.centerY());
        }
    }

    public void focusCameraOnMouse(float mouseX, float mouseY) {
        // mouseX/mouseY приходят в экранных координатах UI.
        //
        // contentRoot.worldToLocal(...) делает очень важную вещь:
        // берет текущий transform GraphView (pan + zoom) и переводит экранную точку
        // обратно в локальные координаты contentRoot, то есть в world-space графа.
        //
        // Благодаря этому можно узнать: "какая логическая точка графа находится под курсором?".
        Vector2f mouseInGraphWorld = contentRoot.getLocalMouse(mouseX, mouseY);
        centerCameraOn(mouseInGraphWorld.x, mouseInGraphWorld.y);
    }

    public void moveCameraToWorld(float worldX, float worldY) {
        // offsetX/offsetY в GraphView - это world-координата левого верхнего угла видимой области.
        // То есть здесь мы НЕ "двигаем экран", а задаем, какая часть мира должна быть показана.
        setOffsetX(worldX);
        setOffsetY(worldY);

        // После ручного изменения offset'ов обязательно пересобираем transform contentRoot.
        syncCameraTransform();
    }

    public void centerCameraOn(float worldX, float worldY) {
        // getContentWidth/Height - видимый размер GraphView на экране.
        // Делим на scale, чтобы перевести экранный размер обратно в world-space.
        float halfVisibleWidth = getContentWidth() / (2f * getScale());
        float halfVisibleHeight = getContentHeight() / (2f * getScale());

        // Чтобы точка оказалась в центре, левый верхний угол камеры
        // должен быть смещен на половину видимой world-области назад.
        setOffsetX(worldX - halfVisibleWidth);
        setOffsetY(worldY - halfVisibleHeight);
        syncCameraTransform();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        // Сохраняем стандартный рендер GraphView (в частности, grid background logic родителя).
        super.drawBackgroundAdditional(guiContext);

        // Рисуем связи здесь, а не в drawBackgroundOverlay(...),
        // чтобы линии шли ПОД нодами.
        drawResearchLinks(guiContext);
    }

    private void drawResearchLinks(GUIContext guiContext) {
        guiContext.pose.pushPose();
        guiContext.pose.translate(getContentX(), getContentY(), 0f);
        guiContext.pose.scale(getScale(), getScale(), 1f);
        guiContext.pose.translate(-getOffsetX(), -getOffsetY(), 0f);

        for (ResearchLink link : links) {
            ResearchNode from = findNode(link.fromId());
            ResearchNode to = findNode(link.toId());

            if (from == null || to == null) {
                continue;
            }

            float startX = from.x + from.width;
            float startY = from.centerY();

            float endX = to.x;
            float endY = to.centerY();

            float thickness = 2.0f;

            // Половина толщины нужна для создания идеального нахлеста на углах
            float halfThick = thickness / 2.0f;

            if (Math.abs(startY - endY) < 1.0f) {
                // --- 1. НОДЫ НА ОДНОМ УРОВНЕ ---
                // Рисуем одну прямую линию, тут углов нет, ломаться нечему.
                DrawerHelper.drawLines(
                        guiContext.graphics,
                        List.of(new Vector2f(startX, startY), new Vector2f(endX, endY)),
                        link.startColor(),
                        link.endColor(),
                        thickness
                );
            } else {
                // --- 2. НОДЫ СМЕЩЕНЫ ПО Y ---
                float middleX = startX + (endX - startX) * 0.5f;

                // ОТРЕЗОК 1: Первая горизонталь (от ноды до середины)
                // Удлиняем вправо (+ halfThick), чтобы закрыть внешний угол
                DrawerHelper.drawLines(
                        guiContext.graphics,
                        List.of(
                                new Vector2f(startX, startY),
                                new Vector2f(middleX + halfThick, startY)
                        ),
                        link.startColor(),
                        link.startColor(),
                        thickness
                );

                // ОТРЕЗОК 2: Вертикаль (спуск или подъем)
                // Рисуется ровно от центра до центра
                DrawerHelper.drawLines(
                        guiContext.graphics,
                        List.of(
                                new Vector2f(middleX, startY),
                                new Vector2f(middleX, endY)
                        ),
                        link.startColor(),
                        link.endColor(), // Здесь происходит градиентный переход
                        thickness
                );

                // ОТРЕЗОК 3: Вторая горизонталь (от середины до цели)
                // Удлиняем влево (- halfThick), чтобы закрыть второй внешний угол
                DrawerHelper.drawLines(
                        guiContext.graphics,
                        List.of(
                                new Vector2f(middleX - halfThick, endY),
                                new Vector2f(endX, endY)
                        ),
                        link.endColor(),
                        link.endColor(),
                        thickness
                );
            }
        }

        guiContext.pose.popPose();
    }

    private ResearchNode findNode(String id) {
        // Простейший линейный поиск.
        // Для демо этого достаточно; если нод станет много,
        // можно держать еще и Map<String, ResearchNode>.
        for (ResearchNode node : nodes) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return null;
    }

    private void syncCameraTransform() {
        // Это ручной аналог внутренней логики GraphView.refreshContentTransform().
        //
        // Как работает формула:
        // - сначала translate уводит world влево/вверх на текущий offset камеры;
        // - затем scale увеличивает или уменьшает весь contentRoot.
        //
        // Итог: одни и те же ноды остаются в world-space,
        // а пользователь видит уже "камеру" поверх этого пространства.
        contentRoot.transform(transform -> transform
                .translate(-(getOffsetX() * getScale()), -(getOffsetY() * getScale()))
                .scale(getScale())
        );
    }
}
