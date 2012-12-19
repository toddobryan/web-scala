postToAjax = (editor) -> 
    code = editor.getValue()
    highlightedCode = $('.ace_line')
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
    document.getElementById('editor').style.fontSize='16px';
    editor.commands.addCommand({
        name: 'myCommand',
        bindKey: {win: 'Ctrl-Return',  mac: 'Command-Return'},
        exec: (editor) -> postToAjax(editor)
    })
    editor.getSession().on('change', (e) ->
    	length = editor.session.getLength() * 20
    	$('div#editor').height(length)
    	editor.resize()
    )
    $('div.ace_gutter').width(0)
    $('div.ace_sb').width(0)
	$('#code').submit((e) ->
		postToAjax(editor)
		e.preventDefault()
	)
)