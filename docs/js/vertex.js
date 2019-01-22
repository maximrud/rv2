/**
 * Вершины графа
 * @param {Line[]} lines Линии
 * @param {int} hand Текущий ход
 * @param {Figure[]} figures Найденные фигуры
 * @param {int} type Максимальный шаблон фигуры
 * @param {int} count Количество камней на доске
 * @param {int} attacker Атакующая сторона
 */
function Vertex(lines, hand, figures, type, count, attacker) {

    if (lines instanceof Layout) {
        var layout = lines;
        Layout.call(this, layout.lines, layout.hand, layout.figures, layout.type, layout.count);
    } else {
        Layout.apply(this, arguments.slice(0, arguments.length - 1));
    }
    // Атакующая сторона
    this.attacker = arguments[arguments.length - 1];

    // Состояние решения
    // Ноль - неизвестно, положительное - выигрышь, отрицательно - проигрышь
    // Колиство ходов до проигрыша или выигрыша на один меньше состояния
    this.state = 0;

    // Перспективность позиции
    this.rating = 0;

    // Ходы решения
    this.edges = null;
}

// Наследование
Vertex.prototype = Object.create(Layout.prototype);
Vertex.prototype.constructor = Vertex;


// Провексти оценку позиции
Vertex.estimate = function (layout, attacker, alledges, computetarget, estimatedepth) {
    var root = new Vertex(layout.alignType(Pattern.OPEN_TWO), attacker);
    root.prepare();
    computetarget = computetarget || 35;
    var target = computetarget - root.count + 1;
    estimatedepth = estimatedepth || 17;
    var count = Math.max(Math.min(target - 2, estimatedepth), 0);
    for (var level = 0; level <= count; level++) {
        root.estimate(level, alledges, computetarget);
        if (root.state !== 0) {
            break;
        }
    }
    if ((attacker === layout.hand && root.state < 0)
            || (attacker !== layout.hand && root.state > 0)) {
        root.state = 0;
    }
    if (Math.abs(root.state) > target) {
        root.state = 0;
    }
    return root;
};

// Получить посчитанные ходы
Vertex.prototype.moves = function () {
    if (this.edges === null) {
        if (this.state > 0) {
            if (this.state < 4) {
                return this.gains(Pattern.FOUR);
            } else if (this.state < 6) {
                return this.gains(Pattern.OPEN_THREE);
            }
        } else if (this.state < 0) {
            if (this.state > -5) {
                return this.downs(Pattern.OPEN_FOUR);
            }
        }
    } else {
        var r = [];
        if (this.edges.length > 0) {
            var top = this.edges[0].vertex;
            this.edges.some(function (e) {
                if (e.vertex.state !== top.state) {
                    return true;
                }
                r.push(e.square);
            });
        }
        return r;
    }
    return null;
};

// Подготовка оценки позиции
Vertex.prototype.prepare = function () {
    var top = this.top();
    if (top !== null) {
        // Свой ход: пять, четыре или открытая три
        if (top.hand === this.hand) {
            switch (top.pattern.type) {
                // Победа
                case Pattern.FIVE:
                    this.state = 1;
                    break;
                    // Победа одним ходом
                case Pattern.OPEN_FOUR:
                case Pattern.FOUR:
                    this.state = 2;
                    break;
                    // Победа в три хода
                case Pattern.OPEN_THREE:
                    this.state = 4;
                    break;
                case Pattern.THREE:
                case Pattern.OPEN_TWO:
                    // Мы атакуем - будет продолжение
                    this.state = this.attacker === this.hand ? 0 : 1;
                    break;
                    // Нет фигур для продолжения
                default:
                    this.state = this.attacker === this.hand ? -1 : 1;
            }
        } else {
            // Чужеой ход
            switch (top.pattern.type) {
                // Проигрышь
                case Pattern.FIVE:
                    this.state = -1;
                    break;
                    // Проигрышь через два хода
                case Pattern.OPEN_FOUR:
                    this.state = -3;
                    break;
                    // Будем защищаться
                case Pattern.FOUR:
                case Pattern.OPEN_THREE:
                    this.state = 0;
                    break;
                    // Нет фигур для продолжения
                default:
                    this.state = this.attacker === this.hand ? -1 : 1;
            }
        }
    } else {
        // Нет фигур для продолжения
        this.state = this.attacker === this.hand ? -1 : 1;
    }
    // Рассчитаем рейтинг
    this.rating = this.rate();
};

// Построить дочерние узлы
Vertex.prototype.expand = function (alledges, computetarget) {
    // Рассчитать возможные ходы
    var moves = [];
    var top = this.top();
    // По уровню угрозы
    switch (top.pattern.type) {
        // Пробуем делать вынужденные ходы и защищаться
        case Pattern.FOUR:
            moves = this.downs(Pattern.FOUR);
            break;
            // Пробуем атаковать и/или защищаться
        case Pattern.OPEN_THREE:
            moves = this.gains(Pattern.THREE);
            this.downs(Pattern.OPEN_THREE).forEach(function (square) {
                if (moves.indexOf(square) === -1) {
                    moves.push(square);
                }
            }, this);
            break;
        default:
            // Пробуем атаковать - мы атакующая сторона
            moves = this.gains(Pattern.OPEN_TWO);
    }
    this.edges = [];
    moves.some(function (square) {
        // Создать ребро с вершиной 
        var e = this.makeEdge(square);
        // Проверим достаточную глубину поиска
        var target = computetarget - this.count + 1;
        if (Math.abs(e.vertex.state) > target) {
            e.vertex.state = 0;
            e.vertex.edges = null;
        }
        // Выигрышный ход - достаточно
        if (!alledges && e.vertex.state < 0) {
            this.edges = [e];
            return true;
        }
        this.edges.push(e);
    }, this);
    this.check();
};

// Создать вершину
Vertex.prototype.makeMove = function (square) {
    var layout = Layout.prototype.makeMove.call(this, square);
    return new Vertex(layout, this.attacker);
};

// Создать ребро с вершиной 
Vertex.prototype.makeEdge = function (square) {
    var v = this.makeMove(square);
    v.prepare();
    return new Edge(square, v);
};

// Оценка выполнения вынужденных ходов
Vertex.prototype.estimate = function (level, alledges, computetarget) {
    // Построение дочернего уровня 
    if (this.state === 0 && this.edges === null && level > 0) {
        this.expand(alledges, computetarget);
    }
    // Итерационный вызов дочернего уровня
    if (this.state === 0 && this.edges !== null && level > 1) {
        // Вызов оценки позции на уровень ниже в порядке рейтинга
        this.edges.some(function (e) {
            var v = e.vertex;
            // На уровень ниже нужен только один ход
            v.estimate(level - 1, false, computetarget);
            // Найден победный ход или для защиты найден не проигранный вариант
            if (!alledges && (v.state < 0
                    // Здесь быстрее ищет победу если != WIN и быстрее защиту 
                    || (this.attacker !== this.hand && v.state === 0))) {
                return true;
            }
        }, this);
        this.check();
    }
};

// Проверка состояния дочерних узлов
Vertex.prototype.check = function () {
    if (this.edges !== null && this.edges.length > 0 && this.state !== -128) {
        // Сортировка дочерних узлов по рейтингу
        this.edges.sort(Edge.Comparator);
        // Выбираем лучший узел из дочерних
        var top = this.edges[0].vertex;
        if (this.attacker === this.hand) {
            if (top.state < 0 && top.state !== -128) {
                // Найден победный ход
                this.state = -top.state + 1;
                // Оставляем позиции с лучним ходом
                var es = [];
                this.edges.some(function (edge) {
                    if (edge.vertex.state !== top.state) {
                        return true;
                    }
                    es.push(edge);
                }, this);
                this.edges = es;
            } else if (top.state > 0) {
                // Перебор вариантов завершен, выигрыша нет
                this.state = -1;
                this.edges = null;
            }
        } else {
            if (top.state < 0 && top.state !== -128) {
                // Найдена ничья                    
                this.state = 1;
                this.edges = null;
            } else if (top.state > 0) {
                // Перебор ходов завершен, позиция проиграна
                this.state = -top.state - 1;
                // Оставляем позиции с лучшим ходом и имеющие дочерние 
                var es = [];
                this.edges.forEach(function (edge) {
                    if (edge.vertex.edges !== null ||
                            edge.vertex.state === top.state) {
                        es.push(edge);
                    }
                }, this);
                this.edges = es;
            }
        }
    }
};

// Сравнение состояний
// Меньшее значение соответствует наиболее короткому выигрышу или
// неизвестности с большим рейтингом или наиболее длинному проигрышу
Vertex.prototype.compareTo = function (v) {
    if (v === null) {
        return -1;
    }
    if (v.state === this.state) {
        return this.rating - v.rating;
    }
    if ((v.state >= 0 && this.state <= 0)
            || (v.state <= 0 && this.state >= 0)) {
        return this.state - v.state;
    } else {
        return v.state - this.state;
    }
};

Vertex.Comparator = function (v1, v2) {
    if (v1 === null) {
        return v2 === null ? 0 : 1;
    }
    return v1.compareTo(v2);
};

