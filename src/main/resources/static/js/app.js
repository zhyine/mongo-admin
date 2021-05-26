var App = function () {

    // dataTables语言配置信息
    var lang = {
        "sProcessing": "处理中...",
        "sLengthMenu": "每页 _MENU_ 项",
        "sZeroRecords": "没有匹配结果",
        "sInfo": "当前显示第 _START_ 至 _END_ 项，共 _TOTAL_ 项。",
        "sInfoEmpty": "当前显示第 0 至 0 项，共 0 项",
        "sInfoFiltered": "(由 _MAX_ 项结果过滤)",
        "sInfoPostFix": "",
        "sSearch": "搜索:",
        "sUrl": "",
        "sEmptyTable": "表中数据为空",
        "sLoadingRecords": "载入中...",
        "sInfoThousands": ",",
        "oPaginate": {
            "sFirst": "首页",
            "sPrevious": "上页",
            "sNext": "下页",
            "sLast": "末页",
            "sJump": "跳转"
        },
        "oAria": {
            "sSortAscending": ": 以升序排列此列",
            "sSortDescending": ": 以降序排列此列"
        }
    }

    /**
     * 新增连接
     */
    var handlerAddServer = function (url) {
        $("#modal-default-addServer").modal("show");
        // 绑定删除事件
        $("#btnModalOk-addServer").bind("click", function () {
            var host = document.getElementById("host").value;
            var port = document.getElementById("port").value;
            var name = document.getElementById("name").value;
            var auth = document.getElementById("auth").value;
            console.log("host", host, "port:", port, "name:", name, "auth",auth);
            $.ajax({
                "url": url,
                "type": "POST",
                "dataType": "JSON",
                "data": {"host": host, "port": port, "name": name, "auth": auth},
                success: function (data) {
                    console.log(data);
                    $("#modal-default-addServer").modal("hide");
                    // 请求成功
                    if (data.code === 200) {
                        $('#btnModalOk').bind("click", function () {
                            window.location.reload();
                        })
                    }

                    // 请求失败
                    else {
                        // 确定按钮的事件改为隐藏模态框
                        $("#btnModalOk").bind("click", function () {
                            $("#modal-default").modal("hide");
                        });
                    }

                    // 因为无论如何都需要提示信息，所以这里的模态框是必须调用的
                    $("#modal-message").html(data.msg);
                    $("#modal-default").modal("show");
                }
            });
        });
    };

    var handlerChangeConnection = function (url) {
        console.log(url);
    }

    /**
     * 初始化DataTables
     */
    var handlerInitDataTables = function (url,columns) {
        // console.log(url)
        var _dataTable = $('#dataTable').DataTable({
            paging: true,
            //左下角信息
            info: false,
            destroy: true,
            lengthChange: false,
            ordering: false,
            processing: true,
            searching: false,
            serverSide: true,
            deferRender:true,
            render: "bootstrap",
            ajax: function (data, callback, settings) {
                var param = {};
                param.length = data.length;//页面显示记录条数，在页面显示每页显示多少项的时候
                param.start = data.start;//开始的记录序号
                param.page = (data.start / data.length)+1;//当前页码
                // console.log(param);
                $.ajax({
                    type: "GET",
                    url: url,
                    cache: false,  //禁用缓存
                    data: param,
                    dataType: "json",
                    success: function (result) {
                        // console.log(result);
                        //封装返回数据
                        var returnData = {};
                        returnData.draw = data.draw;//这里直接自行返回了draw计数器,应该由后台返回
                        returnData.total = result.total;
                        returnData.data = result.data;//返回的数据列表
                        // console.log(returnData);
                        callback(returnData);
                    }
                });
            },
            columns: columns,
            language: lang
        });
        return _dataTable;
    };

    /**
     * 执行命令之后再次初始化
     * @param columns
     * @returns {*|jQuery}
     */
    var handlerInitDataTables2 = function (columns) {
        console.log(editor.getValue());
        var url = window.location.pathname + '/code';

        var _dataTable = $('#dataTable').DataTable({
            destroy: true,
            lengthChange: false,
            ordering: false,
            processing: true,
            searching: false,
            serverSide: true,
            deferRender:true,
            render: "bootstrap",
            ajax: function (data, callback, settings) {
                $.ajax({
                    "url": url,
                    "type": "POST",
                    "dataType": "JSON",
                    "data": {"params": editor.getValue()},
                    "success": function (result) {
                        console.log(result.data);
                        // console.log(result);
                        //封装返回数据
                        var returnData = {};
                        returnData.data = result.data;//返回的数据列表
                        // console.log(returnData);
                        callback(returnData);
                    }
                });
            },
            columns: columns,
            language: lang
        });
        return _dataTable;
    }

    /**
     * 显示单条文档数据
     * @param url
     */
    var handlerShowDetail = function (url) {
        $.ajax({
            "url": url,
            "type": "get",
            "dataType": "html",
            success: function (data) {
                console.log(JSON.parse(data).data);
                // console.log(JSON.stringify(data, null, 2));
                if($('.box-info-search').css('display')=='none') {
                    $('.box-info-search').show('fast')
                }
                editor.setValue(JSON.stringify(JSON.parse(data).data, null, 4));
                editor.refresh();
            }
        });
    };

    /**
     * 删除单条文档
     * @param url 删除链接
     * @param id 需要删除数据的 ID
     */
    var handlerDeleteSingle = function (url, msg) {
        // console.log(url);
        // 可选参数
        if (!msg) msg = null;

        // 将 ID 放入数组中，以便和批量删除通用

        $("#modal-message").html(msg == null ? "您确定删除数据项吗？" : msg);
        $("#modal-default").modal("show");
        // 绑定删除事件
        $("#btnModalOk").bind("click", function () {
            del(url);
        });
    };

    /**
     * 更新保存文档
     */
    var handlerSaveDocuments = function () {
        $('.box-info-search').hide('fast')
        // console.log(window.location.pathname);
        var url = window.location.pathname + '/update';

        setTimeout(function () {
            $.ajax({
                "url": url,
                "type": "POST",
                "dataType": "JSON",
                "data": {"params": editor.getValue()},
                "success": function (data) {
                    // console.log(data);
                    //请求成功后，无论成功还是失败都需要弹出模态框
                    $("#btnModalOk").unbind("click");
                    // 请求成功
                    if (data.code === 200) {
                        $('#btnModalOk').bind("click", function () {
                            window.location.reload();
                        })
                    }

                    // 请求失败
                    else {
                        // 确定按钮的事件改为隐藏模态框
                        $("#btnModalOk").bind("click", function () {
                            $("#modal-default").modal("hide");
                        });
                    }

                    // 因为无论如何都需要提示信息，所以这里的模态框是必须调用的
                    $("#modal-message").html(data.msg);
                    $("#modal-default").modal("show");
                }
            });
        },500);
    };

    /**
     * 插入文档
     */
    var handlerInsertDocument = function () {
        var url = window.location.pathname + '/insert';

        setTimeout(function () {
            $.ajax({
                "url": url,
                "type": "POST",
                "dataType": "JSON",
                "data": {"params": editor.getValue()},
                "success": function (data) {
                    // console.log(data);
                    //请求成功后，无论成功还是失败都需要弹出模态框
                    $("#btnModalOk").unbind("click");
                    // 请求成功
                    if (data.code === 200) {
                        $('#btnModalOk').bind("click", function () {
                            window.location.reload();
                        })
                    }

                    // 请求失败
                    else {
                        // 确定按钮的事件改为隐藏模态框
                        $("#btnModalOk").bind("click", function () {
                            $("#modal-default").modal("hide");
                        });
                    }

                    // 因为无论如何都需要提示信息，所以这里的模态框是必须调用的
                    $("#modal-message").html(data.msg);
                    $("#modal-default").modal("show");
                }
            });
        },500);
    }

    /**
     * 导出Json数据
     */
    var handlerExportList = function () {
        window.open(window.location.pathname + '/exportList');
    }

    /**
     * 执行sql语句以便查询
     */
    var handlerExecuteCommand = function () {
        console.log(editor.getValue());
        var url = window.location.pathname + '/code';

        setTimeout(function () {
            $.ajax({
                "url": url,
                "type": "POST",
                "dataType": "JSON",
                "data": {"params": editor.getValue()},
                "success": function (data) {
                    console.log(data.data);

                }
            });
        },500);



    }

    /**
     * 当前私有函数的私有函数,删除数据
     */
    function del(url) {
        $('#modal-default').modal("hide");
        setTimeout(function () {
            $.ajax({
                "url": url,
                "type": "GET",
                "dataType": "JSON",
                "success": function (data) {
                    //请求成功后，无论成功还是失败都需要弹出模态框
                    $("#btnModalOk").unbind("click");
                    console.log(data)
                    // 请求成功
                    if (data.code === 200) {
                        $('#btnModalOk').bind("click", function () {
                            window.location.reload();
                        })
                    }

                    // 请求失败
                    else {
                        // 确定按钮的事件改为隐藏模态框
                        $("#btnModalOk").bind("click", function () {
                            $("#modal-default").modal("hide");
                        });
                    }

                    // 因为无论如何都需要提示信息，所以这里的模态框是必须调用的
                    $("#modal-message").html(data.msg);
                    $("#modal-default").modal("show");

                }
            });
        },500);
    };


    return {
        /**
         * 初始化
         */
        init: function () {

        },
        /**
         * 新增连接
         */
        addServer: function (url) {
            handlerAddServer(url);
        },
        /**
         * 更改数据库连接
         */
        changeConnection: function (url) {
            handlerChangeConnection(url)
        },
        /**
         * 初始化dataTable
         * @param url
         * @returns {jQuery}
         */
        initDataTables: function (url, columns) {
            return handlerInitDataTables(url, columns);
        },
        /**
         * 展示详细信息
         * @param url
         */
        showDetail: function (url) {
            return handlerShowDetail(url);
        },
        /**
         * 删除单笔数据
         * @param url
         */
        deleteOne: function(url, msg) {
            handlerDeleteSingle(url, msg);
        },
        /**
         * 更新保存文档
         */
        saveDocuments: function () {
            handlerSaveDocuments();
        },
        /**
         * 插入文档
         */
        insertDocument: function () {
            handlerInsertDocument();
        },
        /**
         * 导出Json数据
         */
        exportList: function () {
            handlerExportList();
        },
        executeCommand: function () {
            handlerExecuteCommand();
        },
        initDataTables2: function (columns) {
            return handlerInitDataTables2(columns);
        },


    }
}();

$(document).ready(function () {
    App.init();
});