$('document').ready(->
    editor = ace.edit("editor")
    editor.setTheme("ace/theme/chrome")
    editor.getSession().setMode("ace/mode/scala")
    editor.setValue("")    
    $('#code').submit((e) ->
        $('#line').val(editor.getValue())
        $.post('/interpret', $('#code').serialize(), (result) ->
            $('.prev-content').append('<p>&gt; ' + $('#line').val() + '</p>')
            $('.prev-content').append('<p>' + result + '</p>')
            $('#line').val("")
            editor.setValue("")
        )
        e.preventDefault()
    )
)