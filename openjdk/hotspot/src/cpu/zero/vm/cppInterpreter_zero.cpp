/*
 * Copyright 2003-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * Copyright 2007, 2008, 2009 Red Hat, Inc.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

#include "incls/_precompiled.incl"
#include "incls/_cppInterpreter_zero.cpp.incl"

#ifdef CC_INTERP

#define fixup_after_potential_safepoint()       \
  method = istate->method()

#define CALL_VM_NOCHECK(func)                   \
  thread->set_last_Java_frame();                \
  func;                                         \
  thread->reset_last_Java_frame();              \
  fixup_after_potential_safepoint()

void CppInterpreter::normal_entry(methodOop method, intptr_t UNUSED, TRAPS) {
  JavaThread *thread = (JavaThread *) THREAD;
  ZeroStack *stack = thread->zero_stack();

  // Adjust the caller's stack frame to accomodate any additional
  // local variables we have contiguously with our parameters.
  int extra_locals = method->max_locals() - method->size_of_parameters();
  if (extra_locals > 0) {
    if (extra_locals > stack->available_words()) {
      Unimplemented();
    }
    for (int i = 0; i < extra_locals; i++)
      stack->push(0);
  }

  // Allocate and initialize our frame.
  InterpreterFrame *frame = InterpreterFrame::build(stack, method, thread);
  thread->push_zero_frame(frame);

  // Execute those bytecodes!
  main_loop(0, THREAD);
}

void CppInterpreter::main_loop(int recurse, TRAPS) {
  JavaThread *thread = (JavaThread *) THREAD;
  ZeroStack *stack = thread->zero_stack();

  // If we are entering from a deopt we may need to call
  // ourself a few times in order to get to our frame.
  if (recurse)
    main_loop(recurse - 1, THREAD);

  InterpreterFrame *frame = thread->top_zero_frame()->as_interpreter_frame();
  interpreterState istate = frame->interpreter_state();
  methodOop method = istate->method();

  intptr_t *result = NULL;
  int result_slots = 0;

  // Check we're not about to run out of stack
  if (stack_overflow_imminent(thread)) {
    CALL_VM_NOCHECK(InterpreterRuntime::throw_StackOverflowError(thread));
    goto unwind_and_return;
  }

  while (true) {
    // We can set up the frame anchor with everything we want at
    // this point as we are thread_in_Java and no safepoints can
    // occur until we go to vm mode.  We do have to clear flags
    // on return from vm but that is it.
    thread->set_last_Java_frame();

    // Call the interpreter
    if (JvmtiExport::can_post_interpreter_events())
      BytecodeInterpreter::runWithChecks(istate);
    else
      BytecodeInterpreter::run(istate);
    fixup_after_potential_safepoint();

    // Clear the frame anchor
    thread->reset_last_Java_frame();

    // Examine the message from the interpreter to decide what to do
    if (istate->msg() == BytecodeInterpreter::call_method) {
      methodOop callee = istate->callee();

      // Trim back the stack to put the parameters at the top
      stack->set_sp(istate->stack() + 1);

      // Make the call
      Interpreter::invoke_method(callee, istate->callee_entry_point(), THREAD);
      fixup_after_potential_safepoint();

      // Convert the result
      istate->set_stack(stack->sp() - 1);

      // Restore the stack
      stack->set_sp(istate->stack_limit() + 1);

      // Resume the interpreter
      istate->set_msg(BytecodeInterpreter::method_resume);
    }
    else if (istate->msg() == BytecodeInterpreter::more_monitors) {
      int monitor_words = frame::interpreter_frame_monitor_size();

      // Allocate the space
      if (monitor_words > stack->available_words()) {
        Unimplemented();
      }
      stack->alloc(monitor_words * wordSize);

      // Move the expression stack contents
      for (intptr_t *p = istate->stack() + 1; p < istate->stack_base(); p++)
        *(p - monitor_words) = *p;

      // Move the expression stack pointers
      istate->set_stack_limit(istate->stack_limit() - monitor_words);
      istate->set_stack(istate->stack() - monitor_words);
      istate->set_stack_base(istate->stack_base() - monitor_words);

      // Zero the new monitor so the interpreter can find it.
      ((BasicObjectLock *) istate->stack_base())->set_obj(NULL);

      // Resume the interpreter
      istate->set_msg(BytecodeInterpreter::got_monitors);
    }
    else if (istate->msg() == BytecodeInterpreter::return_from_method) {
      // Copy the result into the caller's frame
      result_slots = type2size[method->result_type()];
      assert(result_slots >= 0 && result_slots <= 2, "what?");
      result = istate->stack() + result_slots;
      break;
    }
    else if (istate->msg() == BytecodeInterpreter::throwing_exception) {
      assert(HAS_PENDING_EXCEPTION, "should do");
      break;
    }
    else if (istate->msg() == BytecodeInterpreter::do_osr) {
      // Unwind the current frame
      thread->pop_zero_frame();

      // Remove any extension of the previous frame
      int extra_locals = method->max_locals() - method->size_of_parameters();
      stack->set_sp(stack->sp() + extra_locals);

      // Jump into the OSR method
      Interpreter::invoke_osr(
        method, istate->osr_entry(), istate->osr_buf(), THREAD);
      return;
    }
    else {
      ShouldNotReachHere();
    }
  }

 unwind_and_return:

  // Unwind the current frame
  thread->pop_zero_frame();

  // Pop our local variables
  stack->set_sp(stack->sp() + method->max_locals());

  // Push our result
  for (int i = 0; i < result_slots; i++)
    stack->push(result[-i]);
}

void CppInterpreter::native_entry(methodOop method, intptr_t UNUSED, TRAPS) {
  // Make sure method is native and not abstract
  assert(method->is_native() && !method->is_abstract(), "should be");

  JavaThread *thread = (JavaThread *) THREAD;
  ZeroStack *stack = thread->zero_stack();

  // Allocate and initialize our frame
  InterpreterFrame *frame = InterpreterFrame::build(stack, method, thread);
  thread->push_zero_frame(frame);
  interpreterState istate = frame->interpreter_state();
  intptr_t *locals = istate->locals();

  // Check we're not about to run out of stack
  if (stack_overflow_imminent(thread)) {
    CALL_VM_NOCHECK(InterpreterRuntime::throw_StackOverflowError(thread));
    goto unwind_and_return;
  }

  // Update the invocation counter
  if ((UseCompiler || CountCompiledCalls) && !method->is_synchronized()) {
    thread->set_do_not_unlock();
    InvocationCounter *counter = method->invocation_counter();
    counter->increment();
    if (counter->reached_InvocationLimit()) {
      CALL_VM_NOCHECK(
        InterpreterRuntime::frequency_counter_overflow(thread, NULL));
      if (HAS_PENDING_EXCEPTION)
        goto unwind_and_return;
    }
    thread->clr_do_not_unlock();
  }

  // Lock if necessary
  BasicObjectLock *monitor;
  monitor = NULL;
  if (method->is_synchronized()) {
    monitor = (BasicObjectLock*) istate->stack_base();
    oop lockee = monitor->obj();
    markOop disp = lockee->mark()->set_unlocked();

    monitor->lock()->set_displaced_header(disp);
    if (Atomic::cmpxchg_ptr(monitor, lockee->mark_addr(), disp) != disp) {
      if (thread->is_lock_owned((address) disp->clear_lock_bits())) {
        monitor->lock()->set_displaced_header(NULL);
      }
      else {
        CALL_VM_NOCHECK(InterpreterRuntime::monitorenter(thread, monitor));
        if (HAS_PENDING_EXCEPTION)
          goto unwind_and_return;
      }
    }
  }

  // Get the signature handler
  InterpreterRuntime::SignatureHandler *handler; {
    address handlerAddr = method->signature_handler();
    if (handlerAddr == NULL) {
      CALL_VM_NOCHECK(InterpreterRuntime::prepare_native_call(thread, method));
      if (HAS_PENDING_EXCEPTION)
        goto unlock_unwind_and_return;

      handlerAddr = method->signature_handler();
      assert(handlerAddr != NULL, "eh?");
    }
    if (handlerAddr == (address) InterpreterRuntime::slow_signature_handler) {
      CALL_VM_NOCHECK(handlerAddr =
        InterpreterRuntime::slow_signature_handler(thread, method, NULL,NULL));
      if (HAS_PENDING_EXCEPTION)
        goto unlock_unwind_and_return;
    }
    handler = \
      InterpreterRuntime::SignatureHandler::from_handlerAddr(handlerAddr);
  }

  // Get the native function entry point
  address function;
  function = method->native_function();
  assert(function != NULL, "should be set if signature handler is");

  // Build the argument list
  if (handler->argument_count() * 2 > stack->available_words()) {
    Unimplemented();
  }
  void **arguments;
  void *mirror; {
    arguments =
      (void **) stack->alloc(handler->argument_count() * sizeof(void **));
    void **dst = arguments;

    void *env = thread->jni_environment();
    *(dst++) = &env;

    if (method->is_static()) {
      istate->set_oop_temp(
        method->constants()->pool_holder()->klass_part()->java_mirror());
      mirror = istate->oop_temp_addr();
      *(dst++) = &mirror;
    }

    intptr_t *src = locals;
    for (int i = dst - arguments; i < handler->argument_count(); i++) {
      ffi_type *type = handler->argument_type(i);
      if (type == &ffi_type_pointer) {
        if (*src) {
          stack->push((intptr_t) src);
          *(dst++) = stack->sp();
        }
        else {
          *(dst++) = src;
        }
        src--;
      }
      else if (type->size == 4) {
        *(dst++) = src--;
      }
      else if (type->size == 8) {
        src--;
        *(dst++) = src--;
      }
      else {
        ShouldNotReachHere();
      }
    }
  }

  // Set up the Java frame anchor
  thread->set_last_Java_frame();

  // Change the thread state to _thread_in_native
  ThreadStateTransition::transition_from_java(thread, _thread_in_native);

  // Make the call
  intptr_t result[4 - LogBytesPerWord];
  ffi_call(handler->cif(), (void (*)()) function, result, arguments);

  // Change the thread state back to _thread_in_Java.
  // ThreadStateTransition::transition_from_native() cannot be used
  // here because it does not check for asynchronous exceptions.
  // We have to manage the transition ourself.
  thread->set_thread_state(_thread_in_native_trans);

  // Make sure new state is visible in the GC thread
  if (os::is_MP()) {
    if (UseMembar) {
      OrderAccess::fence();
    }
    else {
      InterfaceSupport::serialize_memory(thread);
    }
  }

  // Handle safepoint operations, pending suspend requests,
  // and pending asynchronous exceptions.
  if (SafepointSynchronize::do_call_back() ||
      thread->has_special_condition_for_native_trans()) {
    JavaThread::check_special_condition_for_native_trans(thread);
    CHECK_UNHANDLED_OOPS_ONLY(thread->clear_unhandled_oops());
  }

  // Finally we can change the thread state to _thread_in_Java.
  thread->set_thread_state(_thread_in_Java);
  fixup_after_potential_safepoint();

  // Clear the frame anchor
  thread->reset_last_Java_frame();

  // If the result was an oop then unbox it and store it in
  // oop_temp where the garbage collector can see it before
  // we release the handle it might be protected by.
  if (handler->result_type() == &ffi_type_pointer) {
    if (result[0])
      istate->set_oop_temp(*(oop *) result[0]);
    else
      istate->set_oop_temp(NULL);
  }

  // Reset handle block
  thread->active_handles()->clear();

 unlock_unwind_and_return:

  // Unlock if necessary
  if (monitor) {
    BasicLock *lock = monitor->lock();
    markOop header = lock->displaced_header();
    oop rcvr = monitor->obj();
    monitor->set_obj(NULL);

    if (header != NULL) {
      if (Atomic::cmpxchg_ptr(header, rcvr->mark_addr(), lock) != lock) {
        monitor->set_obj(rcvr); {
          HandleMark hm(thread);
          CALL_VM_NOCHECK(InterpreterRuntime::monitorexit(thread, monitor));
        }
      }
    }
  }

 unwind_and_return:

  // Unwind the current activation
  thread->pop_zero_frame();

  // Pop our parameters
  stack->set_sp(stack->sp() + method->size_of_parameters());

  // Push our result
  if (!HAS_PENDING_EXCEPTION) {
    stack->set_sp(stack->sp() - type2size[method->result_type()]);

    switch (method->result_type()) {
    case T_VOID:
      break;

    case T_BOOLEAN:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerByte);
#endif
      SET_LOCALS_INT(*(jboolean *) result != 0, 0);
      break;

    case T_CHAR:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerShort);
#endif
      SET_LOCALS_INT(*(jchar *) result, 0);
      break;

    case T_BYTE:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerByte);
#endif
      SET_LOCALS_INT(*(jbyte *) result, 0);
      break;

    case T_SHORT:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerShort);
#endif
      SET_LOCALS_INT(*(jshort *) result, 0);
      break;

    case T_INT:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerInt);
#endif
      SET_LOCALS_INT(*(jint *) result, 0);
      break;

    case T_LONG:
      SET_LOCALS_LONG(*(jlong *) result, 0);
      break;

    case T_FLOAT:
      SET_LOCALS_FLOAT(*(jfloat *) result, 0);
      break;

    case T_DOUBLE:
      SET_LOCALS_DOUBLE(*(jdouble *) result, 0);
      break;

    case T_OBJECT:
    case T_ARRAY:
      SET_LOCALS_OBJECT(istate->oop_temp(), 0);
      break;

    default:
      ShouldNotReachHere();
    }
  }
}

void CppInterpreter::accessor_entry(methodOop method, intptr_t UNUSED, TRAPS) {
  JavaThread *thread = (JavaThread *) THREAD;
  ZeroStack *stack = thread->zero_stack();
  intptr_t *locals = stack->sp();

  // Drop into the slow path if we need a safepoint check
  if (SafepointSynchronize::do_call_back()) {
    normal_entry(method, 0, THREAD);
    return;
  }

  // Load the object pointer and drop into the slow path
  // if we have a NullPointerException
  oop object = LOCALS_OBJECT(0);
  if (object == NULL) {
    normal_entry(method, 0, THREAD);
    return;
  }

  // Read the field index from the bytecode, which looks like this:
  //  0:  aload_0
  //  1:  getfield
  //  2:    index
  //  3:    index
  //  4:  ireturn/areturn
  // NB this is not raw bytecode: index is in machine order
  u1 *code = method->code_base();
  assert(code[0] == Bytecodes::_aload_0 &&
         code[1] == Bytecodes::_getfield &&
         (code[4] == Bytecodes::_ireturn ||
          code[4] == Bytecodes::_areturn), "should do");
  u2 index = Bytes::get_native_u2(&code[2]);

  // Get the entry from the constant pool cache, and drop into
  // the slow path if it has not been resolved
  constantPoolCacheOop cache = method->constants()->cache();
  ConstantPoolCacheEntry* entry = cache->entry_at(index);
  if (!entry->is_resolved(Bytecodes::_getfield)) {
    normal_entry(method, 0, THREAD);
    return;
  }

  // Get the result and push it onto the stack
  switch (entry->flag_state()) {
  case ltos:
  case dtos:
    if (stack->available_words() < 1) {
      Unimplemented();
    }
    stack->alloc(wordSize);
    break;
  }
  if (entry->is_volatile()) {
    switch (entry->flag_state()) {
    case ctos:
      SET_LOCALS_INT(object->char_field_acquire(entry->f2()), 0);
      break;

    case btos:
      SET_LOCALS_INT(object->byte_field_acquire(entry->f2()), 0);
      break;

    case stos:
      SET_LOCALS_INT(object->short_field_acquire(entry->f2()), 0);
      break;

    case itos:
      SET_LOCALS_INT(object->int_field_acquire(entry->f2()), 0);
      break;

    case ltos:
      SET_LOCALS_LONG(object->long_field_acquire(entry->f2()), 0);
      break;

    case ftos:
      SET_LOCALS_FLOAT(object->float_field_acquire(entry->f2()), 0);
      break;

    case dtos:
      SET_LOCALS_DOUBLE(object->double_field_acquire(entry->f2()), 0);
      break;

    case atos:
      SET_LOCALS_OBJECT(object->obj_field_acquire(entry->f2()), 0);
      break;

    default:
      ShouldNotReachHere();
    }
  }
  else {
    switch (entry->flag_state()) {
    case ctos:
      SET_LOCALS_INT(object->char_field(entry->f2()), 0);
      break;

    case btos:
      SET_LOCALS_INT(object->byte_field(entry->f2()), 0);
      break;

    case stos:
      SET_LOCALS_INT(object->short_field(entry->f2()), 0);
      break;

    case itos:
      SET_LOCALS_INT(object->int_field(entry->f2()), 0);
      break;

    case ltos:
      SET_LOCALS_LONG(object->long_field(entry->f2()), 0);
      break;

    case ftos:
      SET_LOCALS_FLOAT(object->float_field(entry->f2()), 0);
      break;

    case dtos:
      SET_LOCALS_DOUBLE(object->double_field(entry->f2()), 0);
      break;

    case atos:
      SET_LOCALS_OBJECT(object->obj_field(entry->f2()), 0);
      break;

    default:
      ShouldNotReachHere();
    }
  }
}

void CppInterpreter::empty_entry(methodOop method, intptr_t UNUSED, TRAPS) {
  JavaThread *thread = (JavaThread *) THREAD;
  ZeroStack *stack = thread->zero_stack();

  // Drop into the slow path if we need a safepoint check
  if (SafepointSynchronize::do_call_back()) {
    normal_entry(method, 0, THREAD);
    return;
  }

  // Pop our parameters
  stack->set_sp(stack->sp() + method->size_of_parameters());
}

bool CppInterpreter::stack_overflow_imminent(JavaThread *thread) {
  // How is the ABI stack?
  address stack_top = thread->stack_base() - thread->stack_size();
  int free_stack = os::current_stack_pointer() - stack_top;
  if (free_stack < StackShadowPages * os::vm_page_size()) {
    return true;
  }

  // How is the Zero stack?
  // Throwing a StackOverflowError involves a VM call, which means
  // we need a frame on the stack.  We should be checking here to
  // ensure that methods we call have enough room to install the
  // largest possible frame, but that's more than twice the size
  // of the entire Zero stack we get by default, so we just check
  // we have *some* space instead...
  free_stack = thread->zero_stack()->available_words() * wordSize;
  if (free_stack < StackShadowPages * os::vm_page_size()) {
    return true;
  }

  return false;
}

InterpreterFrame *InterpreterFrame::build(ZeroStack*       stack,
                                          const methodOop  method,
                                          JavaThread*      thread) {
  int monitor_words =
    method->is_synchronized() ? frame::interpreter_frame_monitor_size() : 0;
  int stack_words = method->is_native() ? 0 : method->max_stack();

  if (header_words + monitor_words + stack_words > stack->available_words()) {
    Unimplemented();
  }

  intptr_t *locals;
  if (method->is_native())
    locals = stack->sp() + (method->size_of_parameters() - 1);
  else
    locals = stack->sp() + (method->max_locals() - 1);

  stack->push(0); // next_frame, filled in later
  intptr_t *fp = stack->sp();
  assert(fp - stack->sp() == next_frame_off, "should be");

  stack->push(INTERPRETER_FRAME);
  assert(fp - stack->sp() == frame_type_off, "should be");

  interpreterState istate =
    (interpreterState) stack->alloc(sizeof(BytecodeInterpreter));
  assert(fp - stack->sp() == istate_off, "should be");

  istate->set_locals(locals);
  istate->set_method(method);
  istate->set_self_link(istate);
  istate->set_prev_link(NULL);
  istate->set_thread(thread);
  istate->set_bcp(method->is_native() ? NULL : method->code_base());
  istate->set_constants(method->constants()->cache());
  istate->set_msg(BytecodeInterpreter::method_entry);
  istate->set_oop_temp(NULL);
  istate->set_mdx(NULL);
  istate->set_callee(NULL);

  istate->set_monitor_base((BasicObjectLock *) stack->sp());
  if (method->is_synchronized()) {
    BasicObjectLock *monitor =
      (BasicObjectLock *) stack->alloc(monitor_words * wordSize);
    oop object;
    if (method->is_static())
      object = method->constants()->pool_holder()->klass_part()->java_mirror();
    else
      object = (oop) locals[0];
    monitor->set_obj(object);
  }

  istate->set_stack_base(stack->sp());
  istate->set_stack(stack->sp() - 1);
  if (stack_words)
    stack->alloc(stack_words * wordSize);
  istate->set_stack_limit(stack->sp() - 1);

  return (InterpreterFrame *) fp;
}

int AbstractInterpreter::BasicType_as_index(BasicType type) {
  int i = 0;
  switch (type) {
    case T_BOOLEAN: i = 0; break;
    case T_CHAR   : i = 1; break;
    case T_BYTE   : i = 2; break;
    case T_SHORT  : i = 3; break;
    case T_INT    : i = 4; break;
    case T_LONG   : i = 5; break;
    case T_VOID   : i = 6; break;
    case T_FLOAT  : i = 7; break;
    case T_DOUBLE : i = 8; break;
    case T_OBJECT : i = 9; break;
    case T_ARRAY  : i = 9; break;
    default       : ShouldNotReachHere();
  }
  assert(0 <= i && i < AbstractInterpreter::number_of_result_handlers,
         "index out of bounds");
  return i;
}

address InterpreterGenerator::generate_empty_entry() {
  if (!UseFastEmptyMethods)
    return NULL;

  return generate_entry((address) CppInterpreter::empty_entry);
}

address InterpreterGenerator::generate_accessor_entry() {
  if (!UseFastAccessorMethods)
    return NULL;

  return generate_entry((address) CppInterpreter::accessor_entry);
}

address InterpreterGenerator::generate_native_entry(bool synchronized) {
  assert(synchronized == false, "should be");

  return generate_entry((address) CppInterpreter::native_entry);
}

address InterpreterGenerator::generate_normal_entry(bool synchronized) {
  assert(synchronized == false, "should be");

  return generate_entry((address) CppInterpreter::normal_entry);
}

address AbstractInterpreterGenerator::generate_method_entry(
    AbstractInterpreter::MethodKind kind) {
  address entry_point = NULL;

  switch (kind) {
  case Interpreter::zerolocals:
  case Interpreter::zerolocals_synchronized:
    break;

  case Interpreter::native:
    entry_point = ((InterpreterGenerator*) this)->generate_native_entry(false);
    break;

  case Interpreter::native_synchronized:
    entry_point = ((InterpreterGenerator*) this)->generate_native_entry(false);
    break;

  case Interpreter::empty:
    entry_point = ((InterpreterGenerator*) this)->generate_empty_entry();
    break;

  case Interpreter::accessor:
    entry_point = ((InterpreterGenerator*) this)->generate_accessor_entry();
    break;

  case Interpreter::abstract:
    entry_point = ((InterpreterGenerator*) this)->generate_abstract_entry();
    break;

  case Interpreter::method_handle:
    entry_point = ((InterpreterGenerator*) this)->generate_method_handle_entry();
    break;

  case Interpreter::java_lang_math_sin:
  case Interpreter::java_lang_math_cos:
  case Interpreter::java_lang_math_tan:
  case Interpreter::java_lang_math_abs:
  case Interpreter::java_lang_math_log:
  case Interpreter::java_lang_math_log10:
  case Interpreter::java_lang_math_sqrt:
    entry_point = ((InterpreterGenerator*) this)->generate_math_entry(kind);
    break;

  default:
    ShouldNotReachHere();
  }

  if (entry_point == NULL)
    entry_point = ((InterpreterGenerator*) this)->generate_normal_entry(false);

  return entry_point;
}

InterpreterGenerator::InterpreterGenerator(StubQueue* code)
 : CppInterpreterGenerator(code) {
   generate_all();
}

// Deoptimization helpers

InterpreterFrame *InterpreterFrame::build(ZeroStack* stack, int size) {
  int size_in_words = size >> LogBytesPerWord;
  assert(size_in_words * wordSize == size, "unaligned");
  assert(size_in_words >= header_words, "too small");

  if (size_in_words > stack->available_words()) {
    Unimplemented();
  }

  stack->push(0); // next_frame, filled in later
  intptr_t *fp = stack->sp();
  assert(fp - stack->sp() == next_frame_off, "should be");

  stack->push(INTERPRETER_FRAME);
  assert(fp - stack->sp() == frame_type_off, "should be");

  interpreterState istate =
    (interpreterState) stack->alloc(sizeof(BytecodeInterpreter));
  assert(fp - stack->sp() == istate_off, "should be");
  istate->set_self_link(NULL); // mark invalid

  stack->alloc((size_in_words - header_words) * wordSize);

  return (InterpreterFrame *) fp;
}

int AbstractInterpreter::layout_activation(methodOop method,
                                           int       tempcount,
                                           int       popframe_extra_args,
                                           int       moncount,
                                           int       callee_param_count,
                                           int       callee_locals,
                                           frame*    caller,
                                           frame*    interpreter_frame,
                                           bool      is_top_frame) {
  assert(popframe_extra_args == 0, "what to do?");
  assert(!is_top_frame || (!callee_locals && !callee_param_count),
         "top frame should have no caller")

  // This code must exactly match what InterpreterFrame::build
  // does (the full InterpreterFrame::build, that is, not the
  // one that creates empty frames for the deoptimizer).
  //
  // If interpreter_frame is not NULL then it will be filled in.
  // It's size is determined by a previous call to this method,
  // so it should be correct.
  //
  // Note that tempcount is the current size of the expression
  // stack.  For top most frames we will allocate a full sized
  // expression stack and not the trimmed version that non-top
  // frames have.

  int header_words        = InterpreterFrame::header_words;
  int monitor_words       = moncount * frame::interpreter_frame_monitor_size();
  int stack_words         = is_top_frame ? method->max_stack() : tempcount;
  int callee_extra_locals = callee_locals - callee_param_count;

  if (interpreter_frame) {
    intptr_t *locals        = interpreter_frame->sp() + method->max_locals();
    interpreterState istate = interpreter_frame->get_interpreterState();
    intptr_t *monitor_base  = (intptr_t*) istate;
    intptr_t *stack_base    = monitor_base - monitor_words;
    intptr_t *stack         = stack_base - tempcount - 1;

    BytecodeInterpreter::layout_interpreterState(istate,
                                                 caller,
                                                 NULL,
                                                 method,
                                                 locals,
                                                 stack,
                                                 stack_base,
                                                 monitor_base,
                                                 NULL,
                                                 is_top_frame);
  }
  return header_words + monitor_words + stack_words + callee_extra_locals;
}

void BytecodeInterpreter::layout_interpreterState(interpreterState istate,
                                                  frame*    caller,
                                                  frame*    current,
                                                  methodOop method,
                                                  intptr_t* locals,
                                                  intptr_t* stack,
                                                  intptr_t* stack_base,
                                                  intptr_t* monitor_base,
                                                  intptr_t* frame_bottom,
                                                  bool      is_top_frame) {
  istate->set_locals(locals);
  istate->set_method(method);
  istate->set_self_link(istate);
  istate->set_prev_link(NULL);
  // thread will be set by a hacky repurposing of frame::patch_pc()
  // bcp will be set by vframeArrayElement::unpack_on_stack()
  istate->set_constants(method->constants()->cache());
  istate->set_msg(BytecodeInterpreter::method_resume);
  istate->set_bcp_advance(0);
  istate->set_oop_temp(NULL);
  istate->set_mdx(NULL);
  if (caller->is_interpreted_frame()) {
    interpreterState prev = caller->get_interpreterState();
    prev->set_callee(method);
    if (*prev->bcp() == Bytecodes::_invokeinterface)
      prev->set_bcp_advance(5);
    else
      prev->set_bcp_advance(3);
  }
  istate->set_callee(NULL);
  istate->set_monitor_base((BasicObjectLock *) monitor_base);
  istate->set_stack_base(stack_base);
  istate->set_stack(stack);
  istate->set_stack_limit(stack_base - method->max_stack() - 1);
}

address CppInterpreter::return_entry(TosState state, int length) {
  ShouldNotCallThis();
}

address CppInterpreter::deopt_entry(TosState state, int length) {
  return NULL;
}

// Helper for (runtime) stack overflow checks

int AbstractInterpreter::size_top_interpreter_activation(methodOop method) {
  return 0;
}

// Helper for figuring out if frames are interpreter frames

bool CppInterpreter::contains(address pc) {
#ifdef PRODUCT
  ShouldNotCallThis();
#else
  return false; // make frame::print_value_on work
#endif // !PRODUCT
}

// Result handlers and convertors

address CppInterpreterGenerator::generate_result_handler_for(
    BasicType type) {
  assembler()->advance(1);
  return ShouldNotCallThisStub();
}

address CppInterpreterGenerator::generate_tosca_to_stack_converter(
    BasicType type) {
  assembler()->advance(1);
  return ShouldNotCallThisStub();
}

address CppInterpreterGenerator::generate_stack_to_stack_converter(
    BasicType type) {
  assembler()->advance(1);
  return ShouldNotCallThisStub();
}

address CppInterpreterGenerator::generate_stack_to_native_abi_converter(
    BasicType type) {
  assembler()->advance(1);
  return ShouldNotCallThisStub();
}

#endif // CC_INTERP
