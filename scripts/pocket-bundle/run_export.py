#!/usr/bin/env python3
"""Run the pinned upstream exporter with bounded CPU verification threads."""
from pathlib import Path
import runpy
import sys
import torch
import onnxruntime as ort

exporter = Path(sys.argv[1])
sys.path.insert(0, str(exporter))
torch.set_num_threads(1)
torch.set_num_interop_threads(1)
original_session = ort.InferenceSession


def bounded_session(*args, **kwargs):
    if 'sess_options' not in kwargs:
        options = ort.SessionOptions()
        options.intra_op_num_threads = options.inter_op_num_threads = 1
        options.add_session_config_entry('session.intra_op.allow_spinning', '0')
        kwargs['sess_options'] = options
    return original_session(*args, **kwargs)


ort.InferenceSession = bounded_session
if sys.argv[2] == '--quantize-main':
    sys.path.insert(0, str(exporter / 'scripts'))
    from quantize import quantize_file
    output = Path(sys.argv[3])
    if not quantize_file(output / 'flow_lm_main.onnx', output / 'flow_lm_main_int8.onnx'):
        raise SystemExit('Main graph quantization failed')
else:
    script = sys.argv[2]
    sys.argv = [script] + sys.argv[3:]
    runpy.run_path(str(exporter / script), run_name='__main__')
