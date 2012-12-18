ajax = (e) ->
    alert(editor.getValue())
    $('#line').value(editor.getValue())
    alert($('#code').serialize())
    ###    
    $.post('/interpret', $('#code').serialize(), (result) ->
        $('.prev-content').append('<p>&gt; ' + editor.getValue() + '</p>')
        $('.prev-content').append('<p>' + result + '</p>')
        editor.setValue("")
    )
    ###
    e.preventDefault()

$('document').ready(->
    $('#code').submit((e) -> ajax(e))
    editor = ace.edit("editor")
    editor.setTheme("ace/theme/chrome")
    editor.getSession().setMode("ace/mode/scala")
    editor.setValue("")
)