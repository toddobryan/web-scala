postToAjax = (editor) -> 
    code = editor.getValue()
    $.post('/interpret', $.param({line : code}), (result) ->
        $('.prev-content').append('<p>&gt; ' + code + '</p>')
        $('.prev-content').append('<p>' + result + '</p>')
        editor.setValue("")
    )

$('document').ready(() ->
    editor = ace.edit("editor")
    editor.setTheme("ace/theme/chrome")
    editor.getSession().setMode("ace/mode/scala")
    editor.setValue("")
    editor.commands.addCommand({
        name: 'myCommand',
        bindKey: {win: 'Ctrl-Return',  mac: 'Command-Return'},
        exec: (editor) -> postToAjax(editor)
    })
	$('#code').submit((e) ->
		postToAjax(editor)
		e.preventDefault()
	)
)