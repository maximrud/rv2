$(function () {

    var board = $('#board')[0];

    board.draw = function () {
        var desk = $(this).find('table.desk');
        var s = '<tbody><tr><th></th>';
        var a = 'a'.charCodeAt(0);
        for (var c = 0; c < 15; c++) {
            var ch = String.fromCharCode(a + c);
            s += '<th>' + ch + '</th>';
        }
        s += '<th></th></tr>';
        for (var r = 0; r < 15; r++) {
            s += '<tr data-row="' + r + '"><th>' + (15 - r) + '</th>';
            for (var c = 0; c < 15; c++) {
                s += '<td class="freecell" data-col="' + c + '">&nbsp;</td>';
            }
            s += '<th>' + (15 - r) + '</th></tr>';
        }
        s += '<tr><th></th>';
        for (var c = 0; c < 15; c++) {
            var ch = String.fromCharCode(a + c);
            s += '<th>' + ch + '</th>';
        }
        s += '<th></th></tr></tbody>';
        desk.html(s);
        board.fill();
    };

    board.put = function (m, ch, color) {
        var $td = $(this).find('table.desk > tbody > tr[data-row="'
                + (m >> 4) + '"] > td[data-col="' + (m & 0xf) + '"]');
        if (ch)
            $td.html(ch);
        if (color)
            $td.removeClass("freecell").addClass(color);
    };

    board.fill = function () {
        var self = this;
        $(self).find('table.desk').removeClass('black white')
                .addClass(this.hand ? 'black' : 'white');
        $(this).find('table.desk > tbody > tr > td').addClass("freecell")
                .removeClass("black white font-weight-bold").html('&nbsp;');
        var r = this.solution.graph, l = this.solution.layout;
        this.record.forEach(function (m, i) {
            self.put(m, i + 1, i % 2 ? 'white' : 'black');
        });
        if (l && l.figures && l.figures.length > 0) {
            var type = l.top.pattern.type, hand = l.top.hand;
            if (type > 0) {
                l.figures.forEach(function (f) {
                    if (type !== f.pattern.type || hand !== f.hand)
                        return true;
                    var ms = l.hand === hand ? f.gains : f.downs;
                    ms.forEach(function (m) {
                        // self.put(m, '&bull;');
                    });
                });
            } else {
                $(this).find('table.desk').removeClass('black white');
                $(this).find('table.desk > tbody > tr > td').removeClass("freecell");
                l.top.moves.forEach(function (m) {
                    $(self).find('table.desk > tbody > tr[data-row="'
                            + (m >> 4) + '"] > td[data-col="' + (m & 0xf) +
                            '"]').addClass('font-weight-bold');
                });
            }
        }
        $(self).find('[data-info="move"]').removeClass('black white')
                .addClass(this.hand ? 'black' : 'white').html(this.record.length + 1);
        $(self).find('[data-info="state"]').text(r ? (r.state > 0 ?
                'Win' : r.state < 0 ? 'Loss' : 'None') : 'None');
        $(self).find('[data-info="count"]').text(r && r.state !== 0 ? Math.abs(r.state) - 1 : 'None');
        $(self).find('[data-info="rating"]').text(r ? r.rating >> 8 : 'None');
        r && r.moves && r.moves.forEach(function (m) {
            self.put(m, '&diams;');
        });
    };

    board.post = function (action) {
        var self = this;
        $(self).find('.line-progress').addClass('active');
        $(self).find('.alert').addClass('d-none');
        $.ajax({
            type: 'POST',
            url: action,
            data: JSON.stringify({
                record: this.record,
                config: this.config()
            }),
            contentType: 'application/json',
            dataType: 'json',
            success: function (solution) {
                $(self).find('.line-progress').removeClass('active');
                self.solution = solution;
                self.fill();
                $('.nav-link[data-action]').parent().removeClass('active');
                $('.nav-link[data-action="' + action + '"]').parent().addClass('active');
            },
            error: function (xhr) {
                $(self).find('.line-progress').removeClass('active');
                $(self).find('.alert').removeClass('d-none').text(xhr.response ?
                        xhr.response.status + " " + xhr.response.mesage :
                        !xhr.statusText || xhr.statusText === 'error' ?
                        'Error ' + xhr.status : xhr.status + ' ' + xhr.statusText);
            }
        });
    };

    board.config = function () {
        return {
            computetarget: parseInt($('#computetarget').val()),
            estimatedepth: parseInt($('#estimatedepth').val()),
            estimatesize: parseInt($('#estimatesize').val()),
            computedepth: parseInt($('#computedepth').val()),
            computesize: parseInt($('#computesize').val()),
            computeedges: parseInt($('#computeedges').val()),
            computebrute: parseInt($('#computebrute').val())
        };
    };

    board.init = function () {
        this.hand = 0;
        this.record = [(7 << 4) | 7];
        this.solution = {};
        this.draw();
    };

    board.move = function (row, col) {
        this.record.push((row << 4) | col);
        this.post('estimate');
        this.hand = (this.hand + 1) % 2;
    };

    board.back = function () {
        if (this.record.length > 0) {
            this.record.pop();
            this.post('estimate');
            this.hand = (this.hand + 1) % 2;
        }
    };

    board.mirror = function () {
        this.post('mirror');
        var r = this.record;
        for (var i = 0; i < r.length; i++) {
            r[i] = (r[i] & 0xf0) | (14 - (r[i] & 0x0f));
        }
    };

    board.rotate = function () {
        this.post('rotate');
        var r = this.record;
        for (var i = 0; i < r.length; i++) {
            r[i] = ((r[i] & 0xf) << 4) | (14 - ((r[i] & 0xf0) >> 4));
        }
    };

    board.download = function () {
        var self = this;
        $(self).find('.line-progress').addClass('active');
        $(self).find('.alert').addClass('d-none');
        $.ajax({
            type: 'GET',
            url: 'store',
            dataType: 'json',
            success: function (solution) {
                $(self).find('.line-progress').removeClass('active');
                var jsonBlob = new Blob([JSON.stringify(solution)], {type: 'application/json;charset=utf-8'});
                saveAs(jsonBlob, 'gomoku.json');
            },
            error: function (xhr) {
                $(self).find('.line-progress').removeClass('active');
                $(self).find('.alert').removeClass('d-none').text(xhr.response ?
                        xhr.response.status + " " + xhr.response.mesage :
                        !xhr.statusText || xhr.statusText === 'error' ?
                        'Error ' + xhr.status : xhr.status + ' ' + xhr.statusText);
            }
        });
    };

    board.init();

    $(board).on('click', 'table.desk > tbody > tr > td.freecell', function () {
        $(this).removeClass('freecell').addClass(board.hand ? 'black' : 'white').html(board.record.length + 1);
        board.move($(this).parent().data('row'), $(this).data('col'));
    });

    $(document).on('click', '[data-action]', function (event) {
        var action = $(this).data('action');
        if (board[action])
            board[action].call(board);
        else
            board.post(action);
        event.preventDefault();
        event.stopPropagation();
    });

});
